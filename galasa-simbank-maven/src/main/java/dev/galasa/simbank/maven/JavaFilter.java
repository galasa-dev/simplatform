package dev.galasa.simbank.maven;

import java.io.File;
import java.io.FilenameFilter;

public class JavaFilter implements FilenameFilter {

    private String senarioName;

    public JavaFilter(String senarioName) {
        this.senarioName = senarioName.toLowerCase();
    }

    @Override
    public boolean accept(File dir, String name) {
        return name.toLowerCase().equals(this.senarioName + ".java");
    }

}