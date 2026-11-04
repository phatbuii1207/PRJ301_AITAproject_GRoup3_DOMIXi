import java.nio.file.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Path source = Paths.get("/workspace/Main.java");
        try {
            Files.delete(source);
            throw new AssertionError("Source deletion unexpectedly succeeded");
        } catch (java.io.IOException expected) {
            if (!Files.isRegularFile(source)) throw new AssertionError("Source disappeared");
            System.out.println("FILE_DELETION_BLOCKED");
        }
        try {
            Files.write(Paths.get("/opt/aita/owned.txt"), new byte[] {1});
            throw new AssertionError("Image filesystem is writable");
        } catch (java.io.IOException expected) {
            System.out.println("ROOTFS_WRITE_BLOCKED");
        }
        if (Files.exists(Paths.get("/var/run/docker.sock"))) throw new AssertionError("Docker socket exposed");
        System.out.println("DOCKER_SOCKET_ABSENT");
    }
}
