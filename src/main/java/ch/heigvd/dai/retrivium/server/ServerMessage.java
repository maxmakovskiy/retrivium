package ch.heigvd.dai.retrivium.server;

/**
 * List of all the server commands
 *
 * @see TcpServer
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
