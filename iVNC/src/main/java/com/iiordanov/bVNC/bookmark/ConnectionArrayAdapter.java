/*
   ArrayAdapter for bookmark lists

   Copyright 2013 Thincast Technologies GmbH, Author: Martin Fleisz

   This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
   If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/

package com.iiordanov.bVNC.bookmark;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.iiordanov.bVNC.ConnectionBean;
import com.iiordanov.bVNC.R;

import java.util.List;

public class ConnectionArrayAdapter extends ArrayAdapter<ConnectionBean>
{

	public ConnectionArrayAdapter(Context context, int textViewResourceId, List<ConnectionBean> objects)
	{
		super(context, textViewResourceId, objects);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
    	View curView = convertView;
        if (curView == null) 
        {
            LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            curView = vi.inflate(R.layout.bookmark_home_list_item, null);
        }

        ConnectionBean connection = getItem(position);
        TextView label = (TextView) curView.findViewById(R.id.bookmark_home_text1);
        TextView hostname = (TextView) curView.findViewById(R.id.bookmark_home_text2);
        ImageView star_icon = (ImageView) curView.findViewById(R.id.bookmark_home_icon2);
        assert label != null;
        assert hostname != null;

       	label.setText(connection.getNickname());
//       	star_icon.setVisibility(View.GONE);

            hostname.setText(connection.getAddress());

//    	star_icon.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				// start bookmark editor
//				Bundle bundle = new Bundle();
//				String refStr = v.getTag().toString();
//				bundle.putString(BookmarkActivity.PARAM_CONNECTION_REFERENCE, refStr);
//
//				Intent bookmarkIntent = new Intent(getContext(), BookmarkActivity.class);
//				bookmarkIntent.putExtras(bundle);
//				getContext().startActivity(bookmarkIntent);
//			}
//    	});
        return curView;
	}

	public void addItems(List<ConnectionBean> newItems)
	{
		for(ConnectionBean item : newItems)
			add(item);
	}
	
	public void replaceItems(List<ConnectionBean> newItems) 
	{
		clear();
		for(ConnectionBean item : newItems)
			add(item);
	}
	
	public void remove(long bookmarkId)
	{
		for(int i = 0; i < getCount(); i++)
		{
			ConnectionBean bm = getItem(i);
			if(bm.get_Id() == bookmarkId)
			{
				remove(bm);
				return;
			}
		}
	}
}
