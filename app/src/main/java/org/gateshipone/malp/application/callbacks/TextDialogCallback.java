package org.gateshipone.malp.application.callbacks;

/**
 * Callback interface used for the textinput dialog. Used for URL input in CurrentPlaylistView e.g.
 */
public interface TextDialogCallback {
    /**
     * Called when the user dismisses the dialog with "ok" accept button.
     * @param text Text that the user entered
     */
    void onFinished(String text);
}
