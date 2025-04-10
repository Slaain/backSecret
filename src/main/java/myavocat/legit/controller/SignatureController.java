package myavocat.legit.controller;

import jakarta.servlet.http.HttpServletRequest;
import myavocat.legit.model.Signature;
import myavocat.legit.service.SignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/signatures")
public class SignatureController {

    private final SignatureService signatureService;

    @Autowired
    public SignatureController(SignatureService signatureService) {
        this.signatureService = signatureService;
    }

    @PostMapping("/documents/{documentId}/sign")
    public Signature signerDocument(
            @PathVariable UUID documentId,
            @RequestBody Map<String, String> payload,
            HttpServletRequest request
    ) {
        UUID userId = UUID.fromString(payload.get("userId"));
        String imageBase64 = payload.get("imageBase64");

        return signatureService.signerDocument(documentId, userId, imageBase64, request);
    }
}
