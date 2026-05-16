package com.unknown.encrypt.core;

public enum EncryptionDefaultMode {

    /**
     * Only endpoints matched by annotations or explicit URI configuration are processed.
     */
    EXPLICIT,

    /**
     * Process all endpoints unless they are matched by ignore rules.
     */
    ALL
}
