import org.junit.Test;
import java.io.*;

import org.tengel.timescale.TreeReader;

public class TreeReaderTest
{
    @Test
    public void testReadFile()
    {
        try
        {
            // File f = new File("tests/tree3.xml");
            File f = new File("res/raw/timescale.xml");
            TreeReader tr = new TreeReader(new FileInputStream(f));
            tr.buildTable(tr.maxColumns());
            for (int y = 0; y < tr.actualRows(); ++y)
            {
                for (int x = 0; x < tr.actualColumns(); ++x)
                {
                    System.out.print(tr.getCell(x, y).name + "  ");
                }
                System.out.println("");
            }
        }
        catch (Exception e)
        {
            System.out.println("Exception: " +  e.getMessage());
            e.printStackTrace();
        }
    }

}
