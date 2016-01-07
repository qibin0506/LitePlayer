package org.loader.liteplayer.adapter;

import java.util.ArrayList;

import org.loader.liteplayer.R;
import org.loader.liteplayer.application.App;
import org.loader.liteplayer.pojo.SearchResult;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * liteplayer by loader
 * @author qibin
 */
public class SearchResultAdapter extends BaseAdapter {
	private ArrayList<SearchResult> mSearchResult;
	
	public SearchResultAdapter(ArrayList<SearchResult> searchResult) {
		mSearchResult = searchResult;
	}
	
	@Override
	public int getCount() {
		return mSearchResult.size();
	}

	@Override
	public Object getItem(int position) {
		return mSearchResult.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if(convertView == null) {
			convertView = View.inflate(App.sContext, R.layout.search_result_item, null);
			holder = new ViewHolder();
			holder.title = (TextView) convertView.findViewById(R.id.tv_search_result_title);
			holder.artist = (TextView) convertView.findViewById(R.id.tv_search_result_artist);
			holder.album = (TextView) convertView.findViewById(R.id.tv_search_result_album);
			convertView.setTag(holder);
		}else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		String artist = mSearchResult.get(position).getArtist();
		String album = mSearchResult.get(position).getAlbum();
		
		holder.title.setText(mSearchResult.get(position).getMusicName());
		
		if(!TextUtils.isEmpty(artist)) holder.artist.setText(artist);
		else holder.artist.setText("未知艺术家");
		
		if(!TextUtils.isEmpty(album)) holder.album.setText(album);
		else holder.album.setText("未知专辑");
		
		return convertView;
	}
	
	static class ViewHolder {
		public TextView title;
		public TextView artist;
		public TextView album;
	}
}
