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

import android.content.Context;
import android.widget.TableLayout;
import android.app.Activity;
import android.widget.TextView;
import java.lang.Exception;
import android.widget.TableRow;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.util.Log;
import android.util.TypedValue;
import java.util.Vector;
import android.content.ContentValues;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View;
import java.util.HashMap;


public class Table
{
    public static final String TAG = "TimeScale";

    private static Table m_instance = null;

    private Activity                  m_activity;
    private int                       m_minRowHeight;
    private int                       m_displayColumns  = 4;
    private TreeReader                m_tree;
    private boolean                   m_fitScreen       = false;
    private boolean                   m_trueScale       = false;
    private int                       m_startColumn     = 1;
    private int                       m_startRow        = 0;
    private int                       m_endRow          = 0;
    private TableLayout               m_table;
    private int                       m_displayHeight;
    private ScrollView                m_scrollView;
    private String                    m_selectedId;
    private boolean                   m_selectionActive = false;
    private Vector<AnimationDrawable> m_bgAnimations    = new Vector<AnimationDrawable>();
    private Handler                   m_aniHandler      = new Handler();
    private boolean                   m_showExcerpt     = false;
    private String                    m_excerptName;
    private HashMap<String, Double>   m_durations       = new HashMap<String, Double>();
    private int                       m_languageIdx     = 0;
    private String                    m_systemLang;
    private NamesDb                   m_namesDb;


    private class SelectionAnimationRunnable implements Runnable
    {
        public void run()
        {
            for (AnimationDrawable a : m_bgAnimations)
            {
                a.selectDrawable(0);
                a.start();
            }
            if (m_selectionActive)
            {
                m_aniHandler.postDelayed(this, 2000);
            }
        }
    }


    private class MyClickListener implements OnClickListener
    {
        public void onClick(View v)
        {
            drawSection((String)v.getTag());
        }
    }



    private class MyLongClickListener implements OnLongClickListener
    {
        public boolean onLongClick(View v)
        {
            return true;
        }
    }


    private Table(Activity activity)
    {
        m_activity = activity;
        try
        {
            m_tree = new TreeReader(m_activity.getResources().openRawResource(
                                        R.raw.timescale));

            m_tree.buildTable(m_tree.maxColumns());

            // calculate duration of periods
            double lastStart = 0;
            for (int x = 0; x < m_tree.maxColumns(); ++x)
            {
                lastStart = 0;
                for (int y = 0; y < m_tree.actualRows(); ++y)
                {
                    GeoPeriod p = m_tree.getCell(x, y);
                    if (p.state == GeoPeriod.State.NORMAL)
                    {
                        p.duration = p.start - lastStart;
                        m_durations.put(p.nameId, p.duration);
                        lastStart = p.start;
                    }
                }
            }

            m_namesDb = new NamesDb(m_activity.getResources().openRawResource(R.raw.names));
            m_systemLang = m_activity.getResources().getConfiguration().locale.getLanguage();
            m_languageIdx = m_activity.getSharedPreferences(
                    "settings", Context.MODE_PRIVATE).getInt("language-idx", 0);

            TextView dummy = new TextView(m_activity);
            dummy.measure(0, 0);
            m_minRowHeight = dummy.getMeasuredHeight();
        }
        catch (Exception e)
        {
            Log.wtf(Table.TAG, "Startup failed", e);
            InfoDialog.show(activity, "Error",
                            "Startup failed: " + e.getClass().getSimpleName() +
                            "\n" + e.getMessage());
        }
    }


    public static Table instance(Activity activity)
    {
        if (m_instance == null)
        {
            m_instance = new Table(activity);
        }
        else
        {
            m_instance.m_activity = activity;
        }
        return m_instance;
    }


    public void update()
    {
        try
        {
            if (m_tree != null)
            {
                if (m_showExcerpt)
                {
                    drawSection_2nd();
                }
                else
                {
                    drawTable(m_displayColumns);
                }
            }
        }
        catch (Exception e)
        {
            Log.wtf(Table.TAG, "Unable to draw table", e);
            InfoDialog.show(m_activity, "Error", "Unable to draw table: " +
                            e.getClass().getSimpleName() + "\n" + e.getMessage());
        }
    }


    private void drawTable(int columns)
    {
        m_table = new TableLayout(m_activity);
        m_scrollView = (ScrollView) m_activity.findViewById(R.id.ts_scrollview);
        m_scrollView.removeAllViews();
        m_displayColumns = columns;
        try
        {
            addTitle(m_table, columns);
            m_tree.buildTable(columns);
            m_startRow = 0;
            m_endRow = m_tree.actualRows();
            drawTable_2nd();
        }
        catch (Exception e)
        {
            Log.wtf(Table.TAG, "Unable to draw table", e);
            InfoDialog.show(m_activity, "Error", "Unable to draw table: " +
                            e.getClass().getSimpleName() + "\n" + e.getMessage());
        }
    }


    private void drawTable_2nd()
    {
        MyClickListener clickListener = new MyClickListener();
        MyLongClickListener longClickListener = new MyLongClickListener();

        m_activity.getActionBar().setDisplayHomeAsUpEnabled(!isHome());

        try
        {

            // find minDuration and mark cells if selected by search
            double minDuration = Double.MAX_VALUE;
            for (int y = m_startRow; y < m_endRow; ++y)
            {
                for (int x = m_startColumn; x < m_tree.actualColumns(); ++x)
                {
                    GeoPeriod p = m_tree.getCell(x, y);
                    if (p.state == GeoPeriod.State.NORMAL && p.childCount == 0)
                    {
                        p.duration = m_durations.get(p.nameId);
                        minDuration = Math.min(minDuration, p.duration);
                    }
                    if (m_selectedId != null && p.nameId != null &&
                        m_selectionActive && p.nameId.equals(m_selectedId))
                    {
                        p.selected = true;
                    }
                }
            }

            // calculate number of rows to display, find oldest date of start.
            int displayRows = 0;
            double maxMya = 0;
            for (int y = m_endRow - 1;
                 y >= m_startRow && displayRows == 0; --y)
            {
                for (int x = m_startColumn; x < m_tree.actualColumns(); ++x)
                {
                    GeoPeriod p = m_tree.getCell(x, y);
                    if (p.state == GeoPeriod.State.NORMAL)
                    {
                        displayRows = y + 1;
                        maxMya = Math.max(maxMya, p.start);
                        break;
                    }
                }
            }

            if (m_showExcerpt && m_startRow > 0)
            {
                GeoPeriod pp = m_tree.getCell(m_startColumn, m_startRow - 1);
                maxMya = maxMya - pp.start;
            }

            // calculate cellHeight or scale accorindg to settings
            int cellHeight = m_minRowHeight;
            double scale = 0;
            LinearLayout f = (LinearLayout) m_activity.findViewById(
                R.id.rootlayout);
            m_displayHeight = f.getBottom();
            if (m_trueScale == false && m_fitScreen == false)
            {
                cellHeight = m_minRowHeight;
            }
            else if (m_trueScale == false && m_fitScreen == true)
            {
                if (m_displayHeight > 0)
                {
                    cellHeight = (m_displayHeight -
                                  Math.round(1.5f * m_minRowHeight)) / (displayRows - m_startRow);
                }
            }
            else if (m_trueScale == true && m_fitScreen == false)
            {
                scale = m_minRowHeight / minDuration;
            }
            else if (m_trueScale == true && m_fitScreen == true)
            {
                scale = (m_displayHeight -
                         Math.floor(1.5f * m_minRowHeight)) / maxMya;
            }

            for (int y = m_startRow; y < displayRows; ++y)
            {
                TableRow row = new TableRow(m_activity);
                StartLabel startLabel = new StartLabel(m_activity);

                if (m_trueScale == true) // find last NORMAL element of row to
                {                        // calculate cellHeight of row.
                    for (int x = m_startColumn; x < m_tree.actualColumns(); ++x)
                    {
                        GeoPeriod p = m_tree.getCell(x, y);
                        if (p.state == GeoPeriod.State.NORMAL && p.childCount == 0)
                        {
                            cellHeight = (int) Math.round(p.duration * scale);
                        }
                    }
                }

                for (int x = m_startColumn; x < m_tree.actualColumns(); ++x)
                {
                    GeoPeriod p = m_tree.getCell(x, y);
                    TextView tv = new TextView(m_activity);
                    tv.setHeight(cellHeight);
                    switch(p.state)
                    {
                    case NORMAL:
                        tv.setTextColor(0xff000000);
                        tv.setText(translateName(p));
                    case COLOR_ONLY:
                        if (p.color != 0xff000000)
                        {
                            tv.setBackgroundColor(p.color);
                        }
                    case EMPTY:
                    }
                    if (p.selected == true)
                    {
                        AnimationDrawable a = new AnimationDrawable();
                        a.addFrame(new ColorDrawable(0xffffffff), 1000);
                        a.addFrame(new ColorDrawable(p.color), 1000);
                        a.setEnterFadeDuration(500);
                        a.setExitFadeDuration(500);
                        a.setOneShot(true);
                        tv.setBackgroundDrawable(a);
                        m_bgAnimations.add(a);
                    }
                    tv.setClickable(true);
                    tv.setOnClickListener(clickListener);
                    tv.setLongClickable(true);
                    tv.setOnLongClickListener(longClickListener);
                    tv.setTag(p.nameId);

                    row.addView(tv);
                    if (p.state == GeoPeriod.State.NORMAL && p.childCount == 0)
                    {
                        startLabel.setInfo(p);
                    }
                }
                if (cellHeight < m_minRowHeight)
                {
                    startLabel.setHeight(cellHeight);
                }
                for (TextView tv : startLabel.getViews())
                {
                    row.addView(tv);
                }
                m_table.addView(row);
            }
            for (int columnIdx = 0;
                 columnIdx < m_tree.actualColumns() - m_startColumn;
                 ++columnIdx)
            {
                m_table.setColumnStretchable(columnIdx, true);
                m_table.setColumnShrinkable(columnIdx, true);
            }
            m_scrollView.addView(m_table);
        }
        catch (Exception e)
        {
            Log.wtf(Table.TAG, "Unable to draw table", e);
            InfoDialog.show(m_activity, "Error", "Unable to draw table: " +
                            e.getClass().getSimpleName() + "\n" + e.getMessage());
        }
        if (m_selectionActive)
        {
            m_aniHandler.post(new SelectionAnimationRunnable());
        }
    }


    private void addTitle(TableLayout table, int pColumns) throws Exception
    {
        final String[] TITLE = {
            translateName("head_supereon"),
            translateName("head_eon"),
            translateName("head_era"),
            translateName("head_period"),
            translateName("head_epoch"),
            translateName("head_age")};
        TableRow row = new TableRow(m_activity);
        for (int i = m_startColumn - 1; i < pColumns - 1; ++i)
        {
            TextView tv = new TextView(m_activity);
            tv.setText(TITLE[i]);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, m_minRowHeight);
            row.addView(tv);
        }

        TextView mya1 = new TextView(m_activity);
        mya1.setText("m");
        mya1.setTextSize(TypedValue.COMPLEX_UNIT_PX, m_minRowHeight);
        row.addView(mya1);

        TextView mya2 = new TextView(m_activity);
        mya2.setText("ya");
        mya2.setTextSize(TypedValue.COMPLEX_UNIT_PX, m_minRowHeight);
        row.addView(mya2);
        table.addView(row);
    }


    public void addColumn()
    {
        if (m_displayColumns < m_tree.maxColumns())
        {
            m_displayColumns += 1;
            update();
        }
    }


    public void removeColumn()
    {
        if (m_showExcerpt)
        {
            if (m_displayColumns - m_startColumn > 2)
            {
                m_displayColumns -= 1;
                update();
            }
        }
        else
        {
            if (m_displayColumns - m_startColumn > 1)
            {
                m_displayColumns -= 1;
                update();
            }
        }
    }


    public boolean toggleTrueScale()
    {
        m_trueScale = ! m_trueScale;
        update();
        return m_trueScale;
    }


    public boolean toggleFitScreen()
    {
            m_fitScreen = !m_fitScreen;
            update();
            return m_fitScreen;
    }


    public boolean getTrueScale()
    {
        return m_trueScale;
    }


    public boolean getFitScreen()
    {
        return m_fitScreen;
    }


    public void shiftRight()
    {
        if (m_displayColumns < m_tree.maxColumns() && !m_showExcerpt)
        {
            m_startColumn = m_startColumn + 1;
            addColumn();
        }
    }


    public void shiftLeft()
    {
        if (m_startColumn > 1 && !m_showExcerpt)
        {
            m_startColumn = m_startColumn - 1;
            removeColumn();
        }
    }


    private String translateName(GeoPeriod p) throws Exception
    {
        if (p.name == null)
        {
            p.name = translateName(p.nameId);
        }
        return p.name;
    }


    private String translateName(String id) throws Exception
    {
        int langIdx = m_languageIdx;
        if (m_languageIdx == 0)
        {
            if (m_systemLang.equals("de"))
            {
                langIdx = 2;
            }
            else if (m_systemLang.equals("zh"))
            {
                langIdx = 3;
            }
            else if (m_systemLang.equals("nb"))
            {
                langIdx = 4;
            }
            else if (m_systemLang.equals("es"))
            {
                langIdx = 5;
            }
            else
            {
                langIdx = 1;
            }
        }
        return m_namesDb.get(id, langIdx);
    }


    private class Scroller implements Runnable
    {
        private int m_ypos;
        public Scroller(int ypos)
        {
            m_ypos = ypos;
        }
        public void run()
        {
            m_scrollView.scrollTo(1, m_table.getChildAt(m_ypos).getBottom() -
                                  (m_displayHeight / 2));
        }
    }


    public void scrollTo(String nameId)
    {
        int xCol = m_tree.getPosX(nameId);
        m_displayColumns = Math.min(xCol + 2, m_tree.maxColumns());
        m_startColumn    = Math.max(xCol - 1, 1);
        m_selectedId     = nameId;
        drawTable(m_displayColumns);

        int yRow = m_tree.getPosY(m_startColumn, nameId);
        m_table.post(new Scroller(yRow));
    }


    public ContentValues[] getSearchData()
    {
        Vector<ContentValues> cv = new Vector<ContentValues>();
        for (String nameId : m_tree.getSearchData())
        {
            ContentValues v = new ContentValues();
            v.put("nameId", nameId);
            try
            {
                v.put("name", translateName(nameId));
            }
            catch (Exception e)
            {
                Log.wtf(Table.TAG,
                        "No translation for string \"" + nameId + "\"", e);
                InfoDialog.show(m_activity, "Error",
                                "No translation for string \"" + nameId + "\" " +
                                e.getClass().getSimpleName() + "\n" +
                                e.getMessage());
                v.put("name", nameId);
            }
            cv.add(v);
        }
        ContentValues[] cva = new ContentValues[cv.size()];
        return cv.toArray(cva);
    }


    public void setSelectionActive(boolean selectionActive)
    {
        m_selectionActive = selectionActive;
        if (selectionActive == false)
        {
            m_bgAnimations.clear();
        }
    }


    public boolean getSelectionActive()
    {
        return m_selectionActive;
    }


    private void drawSection(String name)
    {
        // find the column which contains the desired period
        columnSearch:
        for (int y = 0; y < m_tree.actualRows(); ++y)
        {
            for (int x = m_startColumn; x < m_tree.actualColumns(); ++x)
            {
                GeoPeriod p = m_tree.getCell(x, y);
                if (p.nameId != null && p.nameId.equals(name))
                {
                    m_startColumn    = x;
                    m_displayColumns = Math.min(x + 3, m_tree.maxColumns());
                    break columnSearch;
                }
            }
        }

        m_showExcerpt = true;
        m_excerptName = name;

        try
        {
            drawSection_2nd();
        }
        catch (Exception e)
        {
            Log.wtf(Table.TAG, "Unable to draw table", e);
            InfoDialog.show(m_activity, "Error", "Unable to draw table: " +
                            e.getClass().getSimpleName() + "\n" + e.getMessage());
        }
    }


    private void drawSection_2nd() throws Exception
    {
        m_tree.buildTable(m_displayColumns);

        // find first row to display
        for (int y = 0; y < m_tree.actualRows(); ++y)
        {
            GeoPeriod p = m_tree.getCell(m_startColumn, y);
            if (p.nameId != null && p.nameId.equals(m_excerptName))
            {
                m_startRow = y;
                break;
            }
        }

        // find last row to display
        for (int y = m_startRow; y < m_tree.actualRows(); ++y)
        {
            GeoPeriod p = m_tree.getCell(m_startColumn, y);
            if (p.nameId != null && !p.nameId.equals(m_excerptName))
            {
                m_endRow = y;
                break;
            }
        }

        m_table = new TableLayout(m_activity);
        m_scrollView = (ScrollView) m_activity.findViewById(R.id.ts_scrollview);
        m_scrollView.removeAllViews();

        addTitle(m_table, m_displayColumns);
        m_tree.buildTable(m_displayColumns);
        drawTable_2nd();
    }


    public void resetView()
    {
        m_displayColumns  = 4;
        m_startColumn     = 1;
        m_startRow        = 0;
        m_endRow          = 0;
        m_selectionActive = false;
        m_showExcerpt     = false;
        update();
    }

    public boolean isHome()
    {
        return (m_displayColumns  == 4 && m_fitScreen == false && m_trueScale == false &&
                m_startColumn == 1 && m_startRow == 0 &&
                m_selectionActive == false && m_showExcerpt == false);
    }

    public void setLanguage(int langIdx)
    {
        m_languageIdx = langIdx;
        update();
    }

    public int getLanguage()
    {
        return m_languageIdx;
    }

}
