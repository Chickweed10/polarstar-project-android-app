package com.example.polarstarproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.polarstarproject.Domain.SafeZone;

import java.util.ArrayList;

public class SafeZoneRecyclerViewAdapter extends RecyclerView.Adapter<SafeZoneRecyclerViewAdapter.sViewHolder> {
    String TAG = "RecyclerViewAdapter";

    //리사이클러뷰에 넣을 데이터 리스트
    ArrayList<SafeZone> RangeArrayList;
    Context Rcontext;

    //아이템 클릭 리스너 인터페이스
    public interface OnItemClickListener{
        void onEditClick(View v, int position); //수정
        void onDeleteClick(View v, int position);//삭제
    }
    //리스너 객체 참조 변수
    private OnItemClickListener mListener = null;
    //리스너 객체 참조를 어댑터에 전달 메서드
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    //생성자를 통하여 데이터 리스트 context를 받음
    public SafeZoneRecyclerViewAdapter(ArrayList<SafeZone> arrayList, Context context){
        this.RangeArrayList = arrayList;
        this.Rcontext = context;
    }

    @NonNull
    @Override
    public sViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //자신이 만든 itemview를 inflate한 다음 뷰홀더 생성
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_safezone_setui,parent,false);
        sViewHolder holder = new sViewHolder(view);


        //생선된 뷰홀더를 리턴하여 onBindViewHolder에 전달한다.
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull sViewHolder holder, int position) {
        holder.sfzUsrEntPl.setText(RangeArrayList.get(position).getName());
        holder.sfzUsrSrchPl.setText(RangeArrayList.get(position).getAddress());
    }

    @Override
    public int getItemCount() {
        //데이터 리스트의 크기를 전달해주어야 함
        return RangeArrayList.size();
    }

    public class sViewHolder extends RecyclerView.ViewHolder {
        TextView sfzUsrEntPl, sfzUsrSrchPl;
        //ImageView sfzImg;
        ImageButton sfzEditBtn, sfzDeleteBtn;

        public sViewHolder(@NonNull View itemView) {
            super(itemView);
            sfzUsrEntPl =  itemView.findViewById(R.id.sfzUsrEntPl);
            sfzUsrSrchPl =  itemView.findViewById(R.id.sfzUsrSrchPl);

            //sfzImg = itemView.findViewById(R.id.sfzImg);
            sfzEditBtn = itemView.findViewById(R.id.sfzEditBtn);
            sfzDeleteBtn = itemView.findViewById(R.id.sfzDeleteBtn);

            sfzEditBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition ();
                    if (position!=RecyclerView.NO_POSITION){
                        if (mListener!=null){
                            mListener.onEditClick (view,position);
                        }
                    }
                }
            });

            sfzDeleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition ();
                    if (position!=RecyclerView.NO_POSITION){
                        if (mListener!=null){
                            mListener.onDeleteClick(view,position);
                        }
                    }
                }
            });
        }
    }
}
