package com.akdemya.adapter.inbound.web;

import com.akdemya.adapter.inbound.web.dto.PurchaseInfoResponse;
import com.akdemya.domain.port.in.DownloadPurchaseUseCase;
import com.akdemya.domain.port.in.GetPurchaseInfoUseCase;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/downloads")
public class DownloadController {

    private final DownloadPurchaseUseCase downloadPurchaseUseCase;
    private final GetPurchaseInfoUseCase getPurchaseInfoUseCase;

    public DownloadController(DownloadPurchaseUseCase downloadPurchaseUseCase,
                              GetPurchaseInfoUseCase getPurchaseInfoUseCase) {
        this.downloadPurchaseUseCase = downloadPurchaseUseCase;
        this.getPurchaseInfoUseCase = getPurchaseInfoUseCase;
    }

    @GetMapping("/{token}")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable UUID token) {
        DownloadPurchaseUseCase.Result resolved = downloadPurchaseUseCase.openByToken(token);
        InputStream stream = resolved.stream();
        String filename = resolved.product().getDisplayFilename();

        StreamingResponseBody body = output -> {
            try (InputStream in = stream) {
                StreamUtils.copy(in, output);
            }
        };

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .body(body);
    }

    @GetMapping("/{token}/info")
    public ResponseEntity<PurchaseInfoResponse> info(@PathVariable UUID token) {
        GetPurchaseInfoUseCase.PurchaseInfo info = getPurchaseInfoUseCase.getInfo(token);
        return ResponseEntity.ok(new PurchaseInfoResponse(
            info.email(),
            info.productName(),
            info.status(),
            info.amountCents(),
            info.currency()
        ));
    }
}
