package org.janelia.simview.util;

import java.io.*;

public class NativeLibraryLoader {

    private String libFileName;

    /**
     * Returns the path inside the jar where the requested native, platform-dependent library should be.
     *
     * @param libraryName name of a native library, e.g. 'klb' for 'klb.dll' on Windows and 'libklb.so' on Linux
     * @return platform-dependent resource path
     */
    public String getResourcePath(final String libraryName) {
        String os = System.getProperty("os.name").replace(" ", "_").toLowerCase();
        String prefix = "lib";
        String suffix = "so";
        final int windex = os.indexOf("windows");
        if (windex != -1) {
            os = os.substring( windex, 7 );
            prefix = "";
            suffix = "dll";
        } else if (os.contains("mac_os")) {
            suffix = "dylib";
        }
        libFileName = String.format("%s%s.%s", prefix, libraryName, suffix);
        final String arch = System.getProperty("os.arch").toLowerCase();
        return String.format("/native/%s-%s/%s", arch, os, libFileName);
    }

    /**
     * Unpacks the requested native library to a temporary folder.
     * The folder is platform dependent. On Windows, it is generally %AppData%\Local\Temp.
     *
     * @param libraryName name of a native library, e.g. 'klb' for 'klb.dll' on Windows and 'libklb.so' on Linux
     * @return path to unpacked library
     * @throws java.io.IOException
     */
    public String unpackFromResources(final String libraryName) throws IOException {
        final String resource = getResourcePath(libraryName);
        final InputStream inStream = NativeLibraryLoader.class.getResourceAsStream(resource);
        if (inStream == null)
            throw new IOException(String.format("[KLB] Native library %s not found as resource at %s", libraryName, resource));

        File dst, tmp;
        OutputStream outStream = null;
        try {
            tmp = File.createTempFile("tmp-", "_" + libFileName);
            dst = new File(tmp.getParentFile() + File.separator + libFileName);
            tmp.delete();
            if (dst.exists()) {
                if (!dst.delete())
                    return dst.getAbsolutePath();
            }
            dst.createNewFile();
            dst.deleteOnExit();
            outStream = new FileOutputStream(dst);
            copy(inStream, outStream);
        } finally {
            inStream.close();
            if (outStream != null)
                outStream.close();
        }
        return dst.getAbsolutePath();
    }

    /**
     * Unpacks and loads the requested native library.
     *
     * @param libraryName
     * @throws java.io.IOException
     */
    public String unpackAndLoadFromResources(final String libraryName) throws IOException {
        final String filePath = unpackFromResources(libraryName);
        System.load(filePath);
        return filePath;
    }

    private void copy(final InputStream in, final OutputStream out) throws IOException {
        final byte[] buffer = new byte[32 * 1024];
        int bytesRead = 0;
        while (bytesRead != -1) {
            bytesRead = in.read(buffer);
            if (bytesRead != -1)
                out.write(buffer, 0, bytesRead);
        }
    }
}
