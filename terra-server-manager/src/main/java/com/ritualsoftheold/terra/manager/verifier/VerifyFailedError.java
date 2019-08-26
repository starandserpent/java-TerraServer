package com.ritualsoftheold.terra.manager.verifier;

/**
 * Thrown when Terra's verifier notices unsafe data. Proceeding without
 * said data would very likely crash Terra. Using the data, though,
 * could be much worse. Don't do that.
 * <p>
 * It is advisable that when you encounter this, you let the application crash.
 * Catching this to collect details and rethrowing should be fine, though.
 *
 */
public class VerifyFailedError extends Error {

    private static final long serialVersionUID = 4497297603208865774L;

    public VerifyFailedError(String msg) {
        super(msg);
    }
}
