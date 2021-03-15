package com.example.musicplayer.adapters;

import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.MainActivity;
import com.example.musicplayer.MediaPlayerService;
import com.example.musicplayer.R;
import com.example.musicplayer.StorageUtil;
import com.example.musicplayer.database.DataLoader;
import com.example.musicplayer.database.Songs;
import com.example.musicplayer.nowplaying.NowPlaying;
import com.example.musicplayer.ui.EditTags;
import com.example.musicplayer.ui.albums.AlbumSongsFragment;
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

public class SongAdapter extends SelectableAdapter<SongAdapter.ViewHolder> implements RecyclerViewFastScroller.OnPopupTextUpdate {


    private final ArrayList<Songs> songs;
    private final ItemClicked activity;
    private final Context context;
    private final StorageUtil storage;
    private final Fragment fragment;
    private Toast toast;


    public SongAdapter(Context context, Fragment fragment, ArrayList<Songs> songsArrayList){

        songs = songsArrayList;
        this.context = context;
        activity = (ItemClicked) fragment;
        this.fragment = fragment;
        storage = new StorageUtil(context);

    }

    @NotNull
    @Override
    public CharSequence onChange(int i) {

        SharedPreferences sort = context.getSharedPreferences("SongSort", Context.MODE_PRIVATE);

        if (sort.getString("sortOrder", MediaStore.Audio.Media.DEFAULT_SORT_ORDER).equals(MediaStore.Audio.Media.DEFAULT_SORT_ORDER)) {
            String s;
            int x = (int) songs.get(i).getTitle().toUpperCase().charAt(0);

            if (songs.get(i).getTitle().toUpperCase().indexOf("THE ") == 0) {
                s = songs.get(i).getTitle().substring(4, 5).toUpperCase();
            } else if (songs.get(i).getTitle().toUpperCase().indexOf("A ") == 0) {
                s = songs.get(i).getTitle().substring(2, 3).toUpperCase();
            }else if (songs.get(i).getTitle().toUpperCase().indexOf("\'") == 0 || songs.get(i).getTitle().toUpperCase().indexOf("\"") == 0) {
                s = songs.get(i).getTitle().substring(1, 2).toUpperCase();
            } else if (!(x >= 65 && x <= 90)) s = "#";
            else {
                s = songs.get(i).getTitle().substring(0, 1).toUpperCase();
            }
            return s;
        }
        return "";
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{


        private final TextView tv_song_name, tv_artist_name, tv_song_duration;
        private final ImageView iv_album_art, iv_popup_menu;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);


            tv_song_name = itemView.findViewById(R.id.tv_song_name);
            tv_artist_name = itemView.findViewById(R.id.tv_artist_name);
            tv_song_duration = itemView.findViewById(R.id.tv_song_duration);
            iv_album_art = itemView.findViewById(R.id.iv_album_art);
            iv_popup_menu = itemView.findViewById(R.id.iv_popup_menu);

           itemView.setOnClickListener(this);
           itemView.setOnLongClickListener(this);

        }


        @Override
        public void onClick(View v) {

            activity.onItemClicked(getAdapterPosition());
            notifyDataSetChanged();
        }

        @Override
        public boolean onLongClick(View v) {
            activity.onItemLongClicked(getAdapterPosition());
            notifyDataSetChanged();
           return true;
        }
    }


    @NonNull
    @Override
    public SongAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_songs,parent,false);
        return new SongAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final SongAdapter.ViewHolder holder, final int position) {

        if (MediaPlayerService.activeAudio != null) {
            if (MediaPlayerService.activeAudio.getId() == songs.get(position).getId()) {
                holder.tv_song_name.setTextColor(context.getResources().getColor(R.color.programmer_green));
                holder.tv_artist_name.setTextColor(context.getResources().getColor(R.color.programmer_green));
            }
            else {
                holder.tv_song_name.setTextColor(context.getResources().getColor(R.color.white));
                holder.tv_artist_name.setTextColor(context.getResources().getColor(R.color.notification_artist));
            }
        }

        new Runnable() {
            @Override
            public void run() {

                holder.itemView.setBackground(isSelected(position) ? context.getResources().getDrawable(R.drawable.selected) : context.getResources().getDrawable(R.drawable.not_selected));

                holder.tv_song_name.setText(songs.get(position).getTitle());
                holder.tv_artist_name.setText(songs.get(position).getArtist());
                holder.tv_song_duration.setText(NowPlaying.getTimeInMins(songs.get(position).getDuration()/1000));
                holder.tv_song_name.setSelected(true);
            }
        }.run();

        new Runnable() {
            @Override
            public void run() {

                Glide.with(context).load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), songs.get(position).getAlbumid()))
                        .error(R.mipmap.cassette_image_foreground)
                        .placeholder(R.mipmap.cassette_image_foreground)
                        .centerCrop()
                        .fallback(R.mipmap.cassette_image_foreground)
                        .into(holder.iv_album_art);
            }
        }.run();


        if (!isSelected(position)) {
            holder.iv_popup_menu.setClickable(true);
            holder.iv_popup_menu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    PopupMenu menu = new PopupMenu(context, v);

                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            switch (item.getTitle().toString()) {

                                case "Play":
                                    if(fragment.getClass().getSimpleName().equals(AlbumSongsFragment.class.getSimpleName())) MainActivity.secondary_index = position;
                                    else MainActivity.index = position;
                                    ArrayList<Songs> playSong = new ArrayList<>();
                                    playSong.add(songs.get(position));
                                    DataLoader.playAudio(0, playSong, storage, context);
                                    break;

                                case "Enqueue":
                                    MediaPlayerService.audioList.add(songs.get(position));
                                    storage.storeAudio(MediaPlayerService.audioList);
                                    break;

                                case "Play next":
                                    MediaPlayerService.audioList.add(MediaPlayerService.audioIndex + 1, songs.get(position));
                                    storage.storeAudio(MediaPlayerService.audioList);
                                    break;

                                case "Shuffle":
                                    if(fragment.getClass().getSimpleName().equals(AlbumSongsFragment.class.getSimpleName())) MainActivity.secondary_index = position;
                                    else MainActivity.index = position;
                                    DataLoader.playAudio(position, songs, storage, context);
                                    MediaControllerCompat.getMediaController((AppCompatActivity) context).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                                    break;

                                case "Add to playlist":
                                    ArrayList<Songs> addSong = new ArrayList<>();
                                    addSong.add(songs.get(position));
                                    DataLoader.addToPlaylist(addSong, context, fragment);
                                    break;

                                case "Lyrics":
                                    if(fragment.getClass().getSimpleName().equals(AlbumSongsFragment.class.getSimpleName())) MainActivity.secondary_index = position;
                                    else MainActivity.index = position;
                                    Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                                    intent.putExtra("query", songs.get(position).getTitle() + " " + songs.get(position).getArtist() + " lyrics");
                                    context.startActivity(intent);
                                    break;

                                case "Edit tags":
                                    if(fragment.getClass().getSimpleName().equals(AlbumSongsFragment.class.getSimpleName())) MainActivity.secondary_index = position;
                                    else MainActivity.index = position;
                                    EditTags.song = songs.get(position);
                                    fragment.startActivityForResult(new Intent(context, EditTags.class), 421);
                                    break;

                                case "Use as ringtone":

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        if (!Settings.System.canWrite(context)) {

                                            if(fragment.getClass().getSimpleName().equals(AlbumSongsFragment.class.getSimpleName())) MainActivity.secondary_index = position;
                                            else MainActivity.index = position;
                                            Intent ringtoneIntent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                            ((AppCompatActivity)context).startActivityForResult(ringtoneIntent, 69);

                                        } else {
                                            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songs.get(position).getId()));
                                            if (toast != null) toast.cancel();
                                            toast = Toast.makeText(context, songs.get(position).getTitle() + " set as Ringtone!", Toast.LENGTH_LONG);
                                            toast.show();
                                        }
                                    } else {
                                        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songs.get(position).getId()));
                                        if (toast != null) toast.cancel();
                                        toast = Toast.makeText(context, songs.get(position).getTitle() + " set as Ringtone!", Toast.LENGTH_LONG);
                                        toast.show();
                                    }
                                    break;

                                case "Delete":
                                    deleteSongs(position);
                                    break;

                            }

                            return true;
                        }
                    });

                    menu.inflate(R.menu.song_popup_menu);
                    menu.show();

                }
            });
        }
        else holder.iv_popup_menu.setClickable(false);
    }




    @Override
    public int getItemCount() {
        return songs.size();
    }


    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.iv_album_art.setImageDrawable(null);
        holder.tv_song_name.setText(null);
        holder.tv_artist_name.setText(null);
        holder.iv_popup_menu.setOnClickListener(null);

        super.onViewRecycled(holder);
    }


    private void deleteSongs(final int position){

        final Dialog dialog = new Dialog(context);

        dialog.setContentView(R.layout.delete_song_layout);

        TextView delete = dialog.findViewById(R.id.tv_delete);

        delete.setText("Are you sure you want to delete " + songs.get(position).getTitle() + "?");

        dialog.show();

        dialog.findViewById(R.id.delete_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();

            }
        });

        dialog.findViewById(R.id.delete_delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                dialog.dismiss();
                context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "_id =? ", new String[]{songs.get(position).getId() + ""});
                new File(songs.get(position).getData()).delete();
                songs.remove(position);
                notifyDataSetChanged();
            }
        });



    }


}
