/*
 * Copyright (C) 2018 Timo Engel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.tengel.timescale;

import android.content.ContentProvider;
import android.net.Uri;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.content.ContentValues;
import android.app.SearchManager;
import android.provider.BaseColumns;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Locale;


public class SearchProvider extends ContentProvider
{
    HashMap<String, Object[]> m_names = new HashMap<String, Object[]>();
    public static final Uri URI = Uri.parse("content://org.tengel.timescale.search/suggestions");


    @Override
    public boolean onCreate()
    {
        return false;
    }


    @Override
    public Cursor query (Uri uri, String[] projection, String selection,
                         String[] selectionArgs, String sortOrder)
    {
        MatrixCursor cursor = new MatrixCursor(
            new String[] {BaseColumns._ID,
                          SearchManager.SUGGEST_COLUMN_TEXT_1,
                          SearchManager.SUGGEST_COLUMN_INTENT_DATA});
        TreeSet<String> ts = new TreeSet<String>(m_names.keySet());
        for (String name : ts)
        {
            if (name.toLowerCase(Locale.US).contains(
                    uri.getLastPathSegment().toLowerCase(Locale.US)))
            {
                cursor.addRow(m_names.get(name));
            }
        }
        return cursor;
    }


    @Override
    public Uri insert (Uri uri, ContentValues values)
    {
        String name = values.getAsString("name");
        Object[] o = new Object[]{m_names.size(),
                                  name,
                                  values.getAsString("nameId")};
        if (name != null)
        {
            m_names.put(name, o);
        }
        return null;
    }


    @Override
    public int update (Uri uri, ContentValues values, String selection,
                       String[] selectionArgs)
    {
        return 0;
    }


    @Override
    public int delete (Uri uri, String selection, String[] selectionArgs)
    {
        int rows = m_names.size();
        m_names.clear();
        return rows;
    }


    @Override
    public String getType (Uri uri)
    {
        return "";
    }

}
