package assignment.mobile.playmp3;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class SongListAdapter extends ArrayAdapter<Song> {
    private Context mContext;
    private int mResource;
    private int lastPosition = -1;
    private String songsPath;
    private String AVATAR_URL = "http://192.168.1.109/mp3-server/avatar/";

    /**
     * Default constructor for the PersonListAdapter
     * @param context
     * @param resource
     * @param objects
     */
    public SongListAdapter(Context context, int resource, Song[] objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //get the persons information
        String id = getItem(position).getId();
        String name = getItem(position).getName();
        String songUrl = getItem(position).getUrl();
        String singer = getItem(position).getSinger();

        //Create the person object with the information
        Song song = new Song(id, name, songUrl, singer);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent, false);

        ImageView viewSheet = convertView.findViewById(R.id.fullSheet);
        TextView viewSongName = (TextView) convertView.findViewById(R.id.songName);
        TextView viewSongAuthor = (TextView) convertView.findViewById(R.id.songAuthor);

        viewSongName.setText(song.getName());
        viewSongAuthor.setText(song.getSinger());

        Glide.with(mContext).load(AVATAR_URL + id + ".jpg").into(viewSheet);

        return convertView;
    }
}
