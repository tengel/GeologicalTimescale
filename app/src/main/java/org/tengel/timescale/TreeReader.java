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

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.lang.*;
import java.util.*;


public class TreeReader
{
    private Vector<Vector<GeoPeriod>>       m_table;
    private int                             m_maxColumns = 0;
    private final Element                   m_rootEle;
    private final HashMap<String, Integer>  m_xPos = new HashMap<>();
    private final Vector<String>            m_searchData = new Vector<>();


    public TreeReader(InputStream inStream) throws Exception
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(inStream);
        m_rootEle = doc.getDocumentElement();

        countColumns(m_rootEle, 0);
        buildTable(m_maxColumns);

        // collect data for search
        for (int y = 0; y < actualRows(); ++y)
        {
            for (int x = 0; x < m_maxColumns; ++x)
            {
                GeoPeriod gp = m_table.get(x).get(y);
                if (gp.state == GeoPeriod.State.NORMAL)
                {
                    m_searchData.add(gp.nameId);
                    m_xPos.put(gp.nameId, x);
                }
            }
        }
    }


    public void buildTable(int maxDepth) throws Exception
    {
        if (maxDepth > m_maxColumns)
        {
            maxDepth = m_maxColumns;
        }
        m_table = new Vector<>();
        for (int i = 0; i < maxDepth; ++i)
        {
            m_table.add(new Vector<>());
        }
        parse(m_rootEle, 0, maxDepth);
    }


    private void countColumns(Element ele, int depth)
    {
        if (depth + 1 > m_maxColumns)
        {
            m_maxColumns = depth + 1;
        }
        for (Node n1 = ele.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
        {
            if (n1.getNodeType() == Node.ELEMENT_NODE &&
                ((Element) n1).getNodeName().equals("children"))
            {
                Element childrenEle = (Element) n1;
                for (Node n2 = childrenEle.getFirstChild(); n2 != null;
                     n2 = n2.getNextSibling())
                {
                    if (n2.getNodeType() == Node.ELEMENT_NODE &&
                        ((Element) n2).getNodeName().equals("period"))
                    {
                        Element periodEle = (Element) n2;
                        countColumns(periodEle, depth + 1);
                    }
                }
            }
        }
    }


    private int parse(Element ele, int depth, int maxDepth)
    {
        int childCount = 0;
        int rowSum = 0;

        if (depth + 1 < maxDepth)
        {
            for (Node n1 = ele.getFirstChild(); n1 != null;
                 n1 = n1.getNextSibling())
            {
                if (n1.getNodeType() == Node.ELEMENT_NODE &&
                    ((Element) n1).getNodeName().equals("children"))
                {
                    Element childrenEle = (Element) n1;
                    for (Node n2 = childrenEle.getFirstChild(); n2 != null;
                         n2 = n2.getNextSibling())
                    {
                        if (n2.getNodeType() == Node.ELEMENT_NODE &&
                            ((Element) n2).getNodeName().equals("period"))
                        {
                            Element periodEle = (Element) n2;
                            childCount += 1;
                            rowSum = rowSum + parse(periodEle, depth + 1,
                                                    maxDepth);
                        }
                    }
                }
            }
        }

        if (childCount == 0)
        {
            rowSum = 1;
        }

        // System.out.println("Tag: " + ele.getNodeName() +
        //                    "  name: " + ele.getAttribute("name") +
        //                    "  children: " + childCount +
        //                    "  depth: " + depth +
        //                    "  rowSum: " + rowSum);

        add(depth, childCount, rowSum, ele);
        return rowSum;
    }


    private void add(int x, int childCount, int rows, Element ele)
    {
        m_table.get(x).add(new GeoPeriod(ele, childCount));
        for (int i = 1; i < rows; ++i)
        {
            m_table.get(x).add(new GeoPeriod(ele,
                                             GeoPeriod.State.COLOR_ONLY,
                                             childCount));
        }

        if (childCount == 0)
        {
            for (int i = x + 1; i < m_table.size(); ++i)
            {
                m_table.get(i).add(new GeoPeriod());
            }
        }
    }


    public GeoPeriod getCell(int xCol, int yRow)
    {
        return m_table.get(xCol).get(yRow);
    }


    public int actualRows()
    {
        return m_table.get(0).size();
    }


    public int actualColumns()
    {
        return m_table.size();
    }


    public int maxColumns()
    {
        return m_maxColumns;
    }


    public Vector<String> getSearchData()
    {
        return m_searchData;
    }


    public int getPosX(String name)
    {
        return m_xPos.get(name);
    }


    public int getPosY(int startColumn, String name)
    {
        for (int y = 0; y < actualRows(); ++y)
        {
            for (int x = startColumn; x < actualColumns(); ++x)
            {
                GeoPeriod gp = m_table.get(x).get(y);
                if (gp.state == GeoPeriod.State.NORMAL && gp.nameId.equals(name))
                {
                    return y;
                }
            }
        }
        return 0;
    }
}
