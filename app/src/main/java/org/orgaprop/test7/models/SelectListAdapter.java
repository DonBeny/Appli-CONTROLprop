package org.orgaprop.test7.models;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.orgaprop.test7.R;
import org.orgaprop.test7.controllers.activities.SelectListActivity;
import org.orgaprop.test7.databinding.AgenceItemBinding;
import org.orgaprop.test7.databinding.ListItemBinding;
import org.orgaprop.test7.databinding.ResidItemBinding;

import java.util.List;

public class SelectListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

   private static final int TYPE_AGC_GRP = 0;
   private static final int TYPE_SEARCH_RSD = 1;

   private final List<SelectItem> items;
   private final String type;
   private final OnItemClickListener listener;

   public interface OnItemClickListener {
      void onItemClick(SelectItem item);
   }

   public SelectListAdapter(List<SelectItem> items, String type, OnItemClickListener listener) {
      this.items = items;
      this.type = type;
      this.listener = listener;
   }

   @Override
   public int getItemViewType(int position) {
      return (type.equals(SelectListActivity.SELECT_LIST_TYPE_AGC) || type.equals(SelectListActivity.SELECT_LIST_TYPE_GRP)) ? TYPE_AGC_GRP : TYPE_SEARCH_RSD;
   }

   @NonNull
   @Override
   public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      LayoutInflater inflater = LayoutInflater.from(parent.getContext());

      if( viewType == TYPE_AGC_GRP ) {
         AgenceItemBinding binding = AgenceItemBinding.inflate(inflater, parent, false);
         return new AgenceViewHolder(binding);
      } else {
         ResidItemBinding binding = ResidItemBinding.inflate(inflater, parent, false);
         return new ResidenceViewHolder(binding);
      }
   }

   @Override
   public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
      SelectItem item = items.get(position);

      if( holder instanceof AgenceViewHolder ) {
         ((AgenceViewHolder) holder).bind(item, listener);
      } else if (holder instanceof ResidenceViewHolder) {
         ((ResidenceViewHolder) holder).bind(item, listener);
      }
   }

   @Override
   public int getItemCount() {
      return items.size();
   }

   static class AgenceViewHolder extends RecyclerView.ViewHolder {
      private final AgenceItemBinding binding;

      public AgenceViewHolder(AgenceItemBinding binding) {
         super(binding.getRoot());
         this.binding = binding;
      }

      public void bind(SelectItem item, OnItemClickListener listener) {
         binding.agenceItemName.setText(item.getName());
         binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
      }
   }

   static class ResidenceViewHolder extends RecyclerView.ViewHolder {
      private final ResidItemBinding binding;

      public ResidenceViewHolder(ResidItemBinding binding) {
         super(binding.getRoot());
         this.binding = binding;
      }

      public void bind(SelectItem item, OnItemClickListener listener) {
         binding.itemResidRef.setText(item.getRef());
         binding.itemResidName.setText(item.getName());
         binding.itemResidEntry.setText(item.getEntry());
         binding.itemResidAdr.setText(item.getAddress() + ", " + item.getPostalCode());
         binding.itemResidCity.setText(item.getCity());
         binding.itemResidLast.setText(item.getLast());
         binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
      }
   }
}
