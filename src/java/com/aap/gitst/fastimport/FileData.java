package com.aap.gitst.fastimport;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import com.aap.gitst.Repo;
import com.starbase.starteam.File;

/**
 * @author Andrey Pavlenko
 */
public class FileData implements FastimportCommand {
    private final File _file;
    private final String _path;
    private java.io.File _checkout;

    public FileData(final File file, final String path) {
        _file = file;
        _path = path;
    }

    public File getFile() {
        return _file;
    }

    public String getPath() {
        return _path;
    }

    public synchronized java.io.File getCheckout() {
        return _checkout;
    }

    public synchronized java.io.File waitForCheckout()
            throws InterruptedException {
        while (_checkout == null) {
            wait();
        }
        return _checkout;
    }

    public synchronized void setCheckout(final java.io.File checkout) {
        if (checkout == null) {
            throw new NullPointerException();
        }
        _checkout = checkout;
        notifyAll();
    }

    @Override
    public void write(final Repo repo, final PrintStream s) throws IOException,
            InterruptedException {
        s.print("data ");
        final java.io.File checkout = waitForCheckout();
        writeFile(checkout, s);
        s.print('\n');
    }

    private static void writeFile(final java.io.File file, final PrintStream s)
            throws IOException {
        s.print(file.length());
        s.print('\n');

        try (InputStream in = new FileInputStream(file)) {
            final byte[] buffer = new byte[8192];
            for (int i = in.read(buffer); i != -1; i = in.read(buffer)) {
                s.write(buffer, 0, i);
            }
        } finally {
            file.delete();
        }
    }
}
