package com.holodos.integrations.googlekeep.infrastructure;

import com.holodos.integrations.googlekeep.application.GoogleKeepClient;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class StubGoogleKeepClient implements GoogleKeepClient {
    @Override
    public KeepSyncResult pushChecklist(String remoteNoteId, List<KeepChecklistItem> items, String lastKnownEtag) {
        String etag = "stub-etag-" + System.currentTimeMillis();
        return new KeepSyncResult(true, etag, "Stub sync successful");
    }

    @Override
    public KeepRemoteState fetchChecklist(String remoteNoteId) {
        return new KeepRemoteState("stub-remote-etag", List.of());
    }
}
