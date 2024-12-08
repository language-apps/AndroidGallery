package com.acorns.acornsmobile;

import java.io.File;
import java.util.Vector;

import android.content.Context;

import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

@SuppressWarnings("FieldCanBeLocal")
public class LessonAdapter extends BaseAdapter
{
	private final String TAG = "Picture Adapter";

    protected MainActivity  context;

    private String[]   pathNames;
    private TextView   selectedItem;
    private int        fontSize;
    protected TextView[] textList;

    /** Constructor to initialize the GridView adapter
     * 
     * @param context The main activity
     * @param files The list of acorns lessons
     * @param fontSize font size of grid items
     */
    LessonAdapter(MainActivity context, Vector<String> files, int fontSize)
    {
       this.context = context;
       this.fontSize = fontSize;
       
       pathNames = new String[files.size()];
       this.textList = new TextView[files.size()];

       this.pathNames = files.toArray(pathNames);
       selectedItem = null;
    }
      
    //@Override
    public int getCount()  // Get count of grid elements
    {
       return pathNames.length;
    }

    /** getter/setter selected grid item */
    private TextView getSelectedItem() { return selectedItem; }
    private void setSelectedItem(TextView view) { selectedItem = view; }

    /** Update a single gridview item */
    private void updateItemAtPosition(GridView grid, int position)
    {
        if (position<0) return;
        int visiblePosition = grid.getFirstVisiblePosition();
        View view = grid.getChildAt(position - visiblePosition);
        grid.getAdapter().getView(position, view, grid);
    }

    @SuppressWarnings("ClickableViewAccessibility")
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
       TextView text;
       if ( convertView == null )
       {
          //Inflate the ImageView layout
          LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
          if (li!=null)
              convertView = li.inflate(R.layout.web, parent, false);

          if (convertView!=null)
          {
              if (textList[position]!=null)
                  text = textList[position];
              else
                  text = convertView.findViewById(R.id.grid_item_text);

              text.setTag(position);
              text.setTextSize(fontSize);
              String fileName = getWebFileName(pathNames[position]);
              text.setText(fileName );
              textList[position] = text;

              text.setOnTouchListener((view, event) -> {
                  if (event.getAction() == MotionEvent.ACTION_DOWN)
                  {
                      TextView lastSelected = getSelectedItem();

                      TextView text1 = (TextView)view;

                      View parent1 = (View)view.getParent();
                      GridView grid = (GridView) parent1.getParent();

                      if (lastSelected!=null)  	{ setSelectedItem(null); 	}
                      else  { setSelectedItem(text1);	}

                      if (text1 != lastSelected)
                      {
                          if (lastSelected != null)
                          {
                              lastSelected.setTextColor(ContextCompat.getColor(context, R.color.unselect));
                              updateItemAtPosition(grid, (Integer)lastSelected.getTag());
                          }

                          text1.setTextColor(ContextCompat.getColor(context, R.color.select));
                          updateItemAtPosition(grid, (Integer) text1.getTag());
                          setSelectedItem(text1);
                      }
                      else
                      {
                          text1.setTextColor(ContextCompat.getColor(context, R.color.unselect));
                          updateItemAtPosition(grid, (Integer) text1.getTag());
                          setSelectedItem(null);
                      }
                  }
                  return true;
              });
          }
          if (convertView==null)
          {
              Log.v(TAG, "Could not create convert view");
              return null;
          }
       }

       Log.v(TAG, "leaving getView()");
       return convertView;
    }
   
    /** Get the name of the acorns lesson file
     * 
     * @param pathName Path to the file on external or internal storage
     * @return The name of the lessons file (without the .html extension).
     */
    private String getWebFileName(String pathName)
    {
    	File file = new File(pathName);
    	String name = file.getName();
    	int offset = name.indexOf(".html");
    	return name.substring(0, offset);
    }

   //@Override
   public Object getItem(int position) 
   {
      if (pathNames==null || position>=pathNames.length)
       	   return null;
   	  else return pathNames[position]; // displayed drawable
   }

   //@Override
   public long getItemId(int position) 
   {
      return position;
   }
   
   /** Update the list of gallery URLs to display
    * @param fontSize The font size of grid items
    *  @param files The new list of Gallery URLs
    */
   void updateDisplay(int fontSize, Vector<String> files)
   {
       selectedItem = null;
       pathNames = new String[files.size()];
       this.fontSize = fontSize;
       this.pathNames = files.toArray(pathNames);

       for (int position = 0; position<files.size(); position++)
       {
           TextView text = textList[position];
           if (text!=null)
           {
               text.setTextSize(fontSize);
               text.setTextColor(ContextCompat.getColor(context, R.color.unselect));
               text.setTag(position);
               String fileName = getWebFileName(pathNames[position]);
               text.setText(fileName );
           }
       }
  }
   
   /** Return the currently selected position */
   int getSelectedLesson()
   {
       if (selectedItem == null) return -1;
	   return (Integer)selectedItem.getTag();
   }

}	// End of LessonAdapter class
