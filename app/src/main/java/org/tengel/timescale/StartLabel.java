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

import java.util.Vector;
import java.text.DecimalFormat;
import android.app.Activity;
import android.widget.TextView;
import android.widget.TableRow.LayoutParams;
import android.view.Gravity;


public class StartLabel
{
    private Activity  m_activity;
    private int       m_height      = 0;
    private String    m_intPart;
    private String    m_fracPart;
    private boolean   m_hasFraction;
    private GeoPeriod m_period      = null;


    public StartLabel(Activity activity)
    {
        m_activity = activity;
    }


    public void setInfo(GeoPeriod periodInfo)
    {
        m_period = periodInfo;
        DecimalFormat myFormatter = new DecimalFormat("0.####");
        String sValue = myFormatter.format(periodInfo.start);
        String[] aValue = sValue.split("\\.");
        if (aValue.length == 2)
        {
            m_intPart  = aValue[0];
            m_fracPart = aValue[1];
            m_hasFraction = true;
        }
        else
        {
            m_intPart  = sValue;
            m_fracPart = "";
            m_hasFraction = false;
        }
    }


    public void setHeight(int height)
    {
        m_height = height;
    }


    public Vector<TextView> getViews()
    {
        Vector<TextView> views = new Vector<TextView>();
        if (m_period != null && m_period.startApprox == true)
        {
            views.add(createTextView("~", Gravity.END));
        }
        else
        {
            views.add(createTextView("", Gravity.END));
        }

        views.add(createTextView(m_intPart, Gravity.END));

        if (m_hasFraction)
        {
            views.add(createTextView(".", Gravity.START));
            views.add(createTextView(m_fracPart, Gravity.START));
        }
        else
        {
            views.add(createTextView(""));
            views.add(createTextView(""));
        }

        if (m_period != null && m_period.accuracy != null)
        {
            views.add(createTextView("±" + m_period.accuracy));
        }
        else
        {
            views.add(createTextView(""));
        }

        if (m_period != null && m_period.gssp == true)
        {
            views.add(createTextView("*"));
        }
        else
        {
            views.add(createTextView(""));
        }
        return views;
    }


    private TextView createTextView(String text)
    {
        return createTextView(text, Gravity.LEFT);
    }


    private TextView createTextView(String text, int gravity)
    {
        TextView tv = new TextView(m_activity);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                           LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM;
        tv.setLayoutParams(lp);
        tv.setGravity(gravity);
        if (m_height > 0)
        {
            tv.setHeight(m_height);
        }
        tv.setText(text);
        return tv;
    }


}
