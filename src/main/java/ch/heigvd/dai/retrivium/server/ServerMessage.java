package ch.heigvd.dai.retrivium.server;

/**
 * List of all commands available from the server CLI
 */
public enum ServerMessage {
    FILES,
    RELEVANT,
    NOTHING_RELEVANT,
    NOTHING_INDEXED,
    CONTENT,
    FILE_DOESNT_EXIST,
    UPLOADED,
    INVALID
}
