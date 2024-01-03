package org.tengel.timescale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

public class LanguageDialog
{

    static class Listener implements DialogInterface.OnClickListener
    {
        private final Activity m_activity;

        Listener(Activity a)
        {
            m_activity = a;
        }

        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            SharedPreferences.Editor spe = m_activity.getSharedPreferences(
                    "settings", Context.MODE_PRIVATE).edit();
            spe.putInt("language-idx", which);
            spe.apply();
            Table.instance(m_activity).setLanguage(which);
            dialog.dismiss();
            m_activity.recreate();
        }
    }

    public Dialog create(Activity activity)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.menu_language);
        builder.setSingleChoiceItems(R.array.languages,
                                     Table.instance(activity).getLanguage(),
                                     new Listener(activity));
        return builder.create();
    }
}
