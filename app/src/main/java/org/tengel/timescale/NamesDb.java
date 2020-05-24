package org.tengel.timescale;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class NamesDb
{
    // 0:id, 1:en, 2:de, 3:zh
    private HashMap<String, String[]> m_names = new HashMap<String, String[]>();

    public NamesDb(InputStream namesStream) throws IOException
    {
        BufferedReader fileReader = new BufferedReader(new InputStreamReader(namesStream));
        while (true)
        {
            String line = fileReader.readLine();
            if (line == null)
            {
                break;
            }
            String[] lItems = line.split("\t");
            if (lItems.length != 4)
            {
                throw new IOException("parse failed: " + line);
            }
            m_names.put(lItems[0], lItems);
        }
    }


    public String get(String id, int langIdx)
    {
        return m_names.get(id)[langIdx];
    }

}
