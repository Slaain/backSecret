package myavocat.legit.service;

import myavocat.legit.dto.OfficeDTO;
import myavocat.legit.model.Office;
import myavocat.legit.repository.OfficeRepository;
import myavocat.legit.repository.UserRepository;
import myavocat.legit.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OfficeService {

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    public boolean existsByName(String name) {
        return officeRepository.existsByName(name);
    }

    @Transactional(readOnly = true)
    public Office getOfficeById(UUID id) {
        return officeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Office not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Office findByName(String name) {
        return officeRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Office not found with name: " + name));
    }

    @Transactional(readOnly = true)
    public List<Office> getOfficesByUser(UUID userId) {
        return officeRepository.findByUsers_Id(userId);
    }

    @Transactional
    public Office createOffice(OfficeDTO officeDTO) {
        if (officeRepository.existsByName(officeDTO.getName())) {
            throw new RuntimeException("Name is already in use");
        }

        Office office = new Office();
        office.setName(officeDTO.getName());
        office.setAddress(officeDTO.getAddress());
        office.setPhone(officeDTO.getPhone());
        office.setEmail(officeDTO.getEmail());
        office.setSiret(officeDTO.getSiret());
        office.setActif(officeDTO.isActif());

        return officeRepository.save(office);
    }

    @Transactional(readOnly = true)
    public List<Office> getAllOffices() {
        return officeRepository.findAll();
    }

    @Transactional
    public Office updateOffice(UUID id, OfficeDTO officeDTO) {
        return officeRepository.findById(id).map(office -> {
            office.setName(officeDTO.getName());
            office.setAddress(officeDTO.getAddress());
            office.setPhone(officeDTO.getPhone());
            office.setEmail(officeDTO.getEmail());
            office.setSiret(officeDTO.getSiret());
            office.setActif(officeDTO.isActif());
            return officeRepository.save(office);
        }).orElseThrow(() -> new RuntimeException("Office not found with id: " + id));
    }

    @Transactional
    public void deleteOffice(UUID id) {
        if (!officeRepository.existsById(id)) {
            throw new RuntimeException("Office not found with id: " + id);
        }
        officeRepository.deleteById(id);
    }
}
