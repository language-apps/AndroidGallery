package com.acorns.acornsmobile;

import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

public class AudioPermission
{
    private static boolean permissionRequested; // -1 = not requested, 1 = denied, 1 = granted

    public static void requestPermission(AppCompatActivity activity)
    {

        if (permissionRequested)
            return;

        String[] perms = {"android.permission.RECORD_AUDIO"};
        int permsRequestCode = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            activity.requestPermissions(perms, permsRequestCode);
        }
        permissionRequested = true;
    }
}
