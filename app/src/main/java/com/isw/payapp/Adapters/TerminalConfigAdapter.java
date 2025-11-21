package com.isw.payapp.Adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.isw.payapp.R;
import com.isw.payapp.model.TerminalConfigModel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TerminalConfigAdapter extends RecyclerView.Adapter<TerminalConfigAdapter.ViewHolder> {
    private static final String TAG = "TerminalConfigAdapter";

    private List<ConfigItem> configItems = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    // Cache for field name to display name mapping
    private static final Map<String, String> DISPLAY_NAME_MAP = new HashMap<>();

    static {
        // Initialize the display name mapping
        DISPLAY_NAME_MAP.put("bank", "Bank Name");
        DISPLAY_NAME_MAP.put("mid", "Merchant ID");
        DISPLAY_NAME_MAP.put("tid", "Terminal ID");
        DISPLAY_NAME_MAP.put("merchantloc", "Merchant Location");
        DISPLAY_NAME_MAP.put("address1", "Address Line 1");
        DISPLAY_NAME_MAP.put("address2", "Address Line 2");
        DISPLAY_NAME_MAP.put("city", "City");
        DISPLAY_NAME_MAP.put("state", "State");
        DISPLAY_NAME_MAP.put("zip", "ZIP Code");
        DISPLAY_NAME_MAP.put("currencycode", "Currency Code");
        DISPLAY_NAME_MAP.put("posCode", "POS Code");
        DISPLAY_NAME_MAP.put("mtype", "Merchant Type");
        DISPLAY_NAME_MAP.put("transip", "TMS IP");
        DISPLAY_NAME_MAP.put("transport", "TMS Port");
        DISPLAY_NAME_MAP.put("keysetid", "Key Set ID");
        DISPLAY_NAME_MAP.put("loginurl", "Login URL");
        DISPLAY_NAME_MAP.put("loginport", "Login port");
    }

    public interface OnItemClickListener {
        void onItemClick(ConfigItem item);
    }

    public static class ConfigItem {
        private final String key;
        private String value;
        private final String displayName;

        public ConfigItem(String key, String value, String displayName) {
            this.key = key;
            this.value = value;
            this.displayName = displayName;
        }

        public String getKey() { return key; }
        public String getValue() { return value; }
        public String getDisplayName() { return displayName; }
        public void setValue(String value) { this.value = value; }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView textKey;
        public final TextView textValue;
        public final ImageView imageEdit;

        public ViewHolder(View itemView) {
            super(itemView);
            textKey = itemView.findViewById(R.id.textKey);
            textValue = itemView.findViewById(R.id.textValue);
            imageEdit = itemView.findViewById(R.id.imageEdit);
        }
    }

    /**
     * Set configuration using explicit field mapping (Recommended)
     */
    public void setConfig(TerminalConfigModel config) {
        configItems.clear();

        // Use explicit mapping for better performance and clarity
        configItems.add(new ConfigItem("bank", getSafeString(config.getBank()), "Bank Name"));
        configItems.add(new ConfigItem("mid", getSafeString(config.getMid()), "Merchant ID"));
        configItems.add(new ConfigItem("tid", getSafeString(config.getTid()), "Terminal ID"));
        configItems.add(new ConfigItem("merchantloc", getSafeString(config.getMerchantloc()), "Merchant Location"));
        configItems.add(new ConfigItem("address1", getSafeString(config.getAddress1()), "Address Line 1"));
        configItems.add(new ConfigItem("address2", getSafeString(config.getAddress2()), "Address Line 2"));
        configItems.add(new ConfigItem("city", getSafeString(config.getCity()), "City"));
        configItems.add(new ConfigItem("state", getSafeString(config.getState()), "State"));
        configItems.add(new ConfigItem("zip", getSafeString(config.getZip()), "ZIP Code"));
        configItems.add(new ConfigItem("currencycode", getSafeString(config.getCurrencycode()), "Currency Code"));
        configItems.add(new ConfigItem("posCode", getSafeString(config.getPosCode()), "POS Code"));
        configItems.add(new ConfigItem("mtype", getSafeString(config.getMtype()), "Merchant Type"));
        configItems.add(new ConfigItem("transip", getSafeString(config.getTransip()), "TMS IP"));
        configItems.add(new ConfigItem("transport", getSafeString(config.getTransport()), "TMS Port"));
        configItems.add(new ConfigItem("keysetid", getSafeString(config.getKeysetid()), "Key Set ID"));
        configItems.add(new ConfigItem("loginurl", getSafeString(config.getLoginurl()), "Login IP"));
        configItems.add(new ConfigItem("loginport", getSafeString(config.getLoginport()), "Login Port"));
        notifyDataSetChanged();
    }

    /**
     * Set configuration using reflection (Alternative method)
     * Note: Consider removing this method if not needed to avoid reflection overhead
     */
    public void setConfigUsingReflection(TerminalConfigModel config) {
        configItems.clear();

        Field[] fields = TerminalConfigModel.class.getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();

            // Skip synthetic fields and common excluded fields
            if (fieldName.startsWith("$") || fieldName.equals("serialVersionUID")) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(config);
                String stringValue = value != null ? value.toString() : "";
                String displayName = convertKeyToDisplayName(fieldName);

                configItems.add(new ConfigItem(fieldName, stringValue, displayName));
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Error accessing field: " + fieldName, e);
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Convert field name to user-friendly display name
     */
    private String convertKeyToDisplayName(String key) {
        return DISPLAY_NAME_MAP.getOrDefault(key,
                key.substring(0, 1).toUpperCase() + key.substring(1));
    }

    /**
     * Safe string getter to handle null values
     */
    private String getSafeString(String value) {
        return value != null ? value : "";
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_terminal_config, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConfigItem item = configItems.get(position);

        holder.textKey.setText(item.getDisplayName());
        holder.textValue.setText(item.getValue());

        // Set click listeners
        View.OnClickListener clickListener = v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        };

        holder.imageEdit.setOnClickListener(clickListener);
        holder.itemView.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return configItems.size();
    }

    /**
     * Get updated configuration model with current values
     */
    public TerminalConfigModel getUpdatedConfig() {
        TerminalConfigModel config = new TerminalConfigModel();

        // Create a map for efficient lookup
        Map<String, String> valueMap = new HashMap<>();
        for (ConfigItem item : configItems) {
            valueMap.put(item.getKey(), item.getValue());
        }

        // Set values using the map
        config.setBank(valueMap.get("bank"));
        config.setMid(valueMap.get("mid"));
        config.setTid(valueMap.get("tid"));
        config.setMerchantloc(valueMap.get("merchantloc"));
        config.setAddress1(valueMap.get("address1"));
        config.setAddress2(valueMap.get("address2"));
        config.setCity(valueMap.get("city"));
        config.setState(valueMap.get("state"));
        config.setZip(valueMap.get("zip"));
        config.setCurrencycode(valueMap.get("currencycode"));
        config.setPosCode(valueMap.get("posCode"));
        config.setMtype(valueMap.get("mtype"));
        config.setTransip(valueMap.get("transip"));
        config.setTransport(valueMap.get("transport"));
        config.setKeysetid(valueMap.get("keysetid"));
        config.setLoginurl(valueMap.get("loginurl"));
        config.setLoginport(valueMap.get("loginport"));
        return config;
    }

    /**
     * Alternative method using reflection (if needed)
     */
    public TerminalConfigModel getUpdatedConfigUsingReflection() {
        TerminalConfigModel config = new TerminalConfigModel();

        Map<String, String> valueMap = new HashMap<>();
        for (ConfigItem item : configItems) {
            valueMap.put(item.getKey(), item.getValue());
        }

        try {
            for (ConfigItem item : configItems) {
                Field field = TerminalConfigModel.class.getDeclaredField(item.getKey());
                field.setAccessible(true);
                field.set(config, item.getValue());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating config via reflection", e);
        }

        return config;
    }

    /**
     * Update a specific configuration item
     */
    public void updateItem(String key, String newValue) {
        for (int i = 0; i < configItems.size(); i++) {
            ConfigItem item = configItems.get(i);
            if (item.getKey().equals(key)) {
                item.setValue(newValue != null ? newValue : "");
                notifyItemChanged(i);
                return;
            }
        }
        Log.w(TAG, "Item with key '" + key + "' not found for update");
    }

    /**
     * Get current configuration items (for testing/debugging)
     */
    public List<ConfigItem> getConfigItems() {
        return new ArrayList<>(configItems);
    }
}