package org.quffencore.qt;

import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;

import org.qtproject.qt5.android.bindings.QtActivity;

import java.io.File;

public class QuffenQtActivity extends QtActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        final File quffenDir = new File(getFilesDir().getAbsolutePath() + "/.quffen");
        if (!quffenDir.exists()) {
            quffenDir.mkdir();
        }

        super.onCreate(savedInstanceState);
    }
}
