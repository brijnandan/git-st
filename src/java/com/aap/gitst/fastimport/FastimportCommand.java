package com.aap.gitst.fastimport;

import java.io.IOException;
import java.io.PrintStream;

import com.aap.gitst.Repo;

/**
 * @author Andrey Pavlenko
 */
public interface FastimportCommand {
    public void write(Repo repo, PrintStream s) throws IOException,
            InterruptedException;
}
