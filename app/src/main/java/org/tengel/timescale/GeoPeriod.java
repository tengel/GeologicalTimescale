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

import org.w3c.dom.Element;
import java.util.Locale;

public class GeoPeriod
{

    public enum State {NORMAL, EMPTY, COLOR_ONLY}

    public String   nameId;
    public String   name        = null;
    public int      color;
    public double   start;
    public boolean  startApprox = false;
    public String   accuracy    = null;
    public boolean  gssp        = false;
    public String   text;
    public State    state;
    public int      childCount;
    public double   duration;
    public boolean  selected    = false;


    public GeoPeriod()
    {
        state = State.EMPTY;
    }


    public GeoPeriod(Element ele, int pChildCount)
    {
        childCount = pChildCount;
        state = State.NORMAL;
        nameId = ele.getAttribute("name");
        color = parseInt("ff" + ele.getAttribute("color"));
        if (ele.hasAttribute("start"))
        {
            start = Double.parseDouble(ele.getAttribute("start"));
        }
        if (ele.hasAttribute("gssp"))
        {
            gssp = ele.getAttribute("gssp").toLowerCase(Locale.US).equals("yes");
        }
        if (ele.hasAttribute("startApprox") &&
            ele.getAttribute("startApprox").equalsIgnoreCase("yes"))
        {
            startApprox = true;
        }
        if (ele.hasAttribute("accuracy"))
        {
            accuracy = ele.getAttribute("accuracy");
        }
    }


    public GeoPeriod(Element ele, GeoPeriod.State pState, int pChildCount)
    {
        this(ele, pChildCount);
        state = pState;
    }


    private int parseInt(String value)
    {
        try
        {
            return (int) (Long.parseLong(value, 16) - 0xffffffff - 1);
        }
        catch(NumberFormatException nfe)
        {
            return 0;
        }
    }
}
