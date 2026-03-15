package com.holodos.integrations.googlekeep.application;

import java.util.List;

public interface GoogleKeepClient {
    KeepSyncResult pushChecklist(String remoteNoteId, List<KeepChecklistItem> items, String lastKnownEtag);
    KeepRemoteState fetchChecklist(String remoteNoteId);

    record KeepChecklistItem(String text, boolean checked, String externalId) {}
    record KeepSyncResult(boolean success, String newEtag, String details) {}
    record KeepRemoteState(String etag, List<KeepChecklistItem> items) {}
}
