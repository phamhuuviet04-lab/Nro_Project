package nro.models.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import nro.models.data.LocalManager;
import nro.models.database.PlayerDAO;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.utils.Logger;

/**
 * Dịch vụ đổi và lưu Set đồ nhanh (Gear Presets) cho Sư phụ và Đệ tử.
 * Hỗ trợ lưu trang bị đang mặc vào Set 1, Set 2 và đổi cả bộ trang bị tức thì.
 */
public class GearPresetService {

    public static final int MENU_GEAR_PRESET = 88_001;
    private static final Gson GSON = new Gson();
    private static final Type PRESET_LIST_TYPE = new TypeToken<List<PresetItem>>() {}.getType();

    private static GearPresetService instance;

    // Cache: key = playerId + "_" + presetId + "_" + isPet
    private final Map<String, List<PresetItem>> cache = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    public static class PresetItem {
        public int slot;
        public long createTime;
        public int tempId;
        public String name;

        public PresetItem() {}

        public PresetItem(int slot, long createTime, int tempId, String name) {
            this.slot = slot;
            this.createTime = createTime;
            this.tempId = tempId;
            this.name = name;
        }
    }

    public static synchronized GearPresetService gI() {
        if (instance == null) {
            instance = new GearPresetService();
            instance.initTable();
        }
        return instance;
    }

    /**
     * Tự động khởi tạo bảng lưu trữ preset trong database nếu chưa có.
     */
    public void initTable() {
        if (initialized) {
            return;
        }
        try (Connection con = LocalManager.getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS player_gear_presets ("
                    + "player_id BIGINT NOT NULL, "
                    + "preset_id INT NOT NULL, "
                    + "is_pet TINYINT NOT NULL DEFAULT 0, "
                    + "items TEXT NOT NULL, "
                    + "PRIMARY KEY (player_id, preset_id, is_pet)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            initialized = true;
        } catch (Exception e) {
            Logger.logException(GearPresetService.class, e, "Lỗi tạo bảng player_gear_presets");
        }
    }

    private String key(long playerId, int presetId, boolean isPet) {
        return playerId + "_" + presetId + "_" + (isPet ? 1 : 0);
    }

    /**
     * Mở Menu chọn Set Đồ dạng Con Mèo (hiển thị popup menu nút bấm trực tiếp trên màn hình).
     */
    public void showMenu(Player player) {
        if (player == null || !player.isPl()) {
            return;
        }
        NpcService.gI().createMenuConMeo(player, MENU_GEAR_PRESET, -1,
                "|7|BỘ TRANG BỊ (SET ĐỒ NHANH)\n"
                + "|1|• Thay toàn bộ đồ cho Sư phụ / Đệ tử chỉ bằng 1 chạm!\n"
                + "|2|• Hỗ trợ lưu Set 1 & Set 2 riêng biệt.",
                "Mặc Set 1\n(Sư phụ)", "Mặc Set 2\n(Sư phụ)",
                "Lưu đồ đang mặc\nvào Set 1 (SP)", "Lưu đồ đang mặc\nvào Set 2 (SP)",
                "Mặc Set 1\n(Đệ tử)", "Mặc Set 2\n(Đệ tử)",
                "Lưu đồ đang mặc\nvào Set 1 (Đệ)", "Lưu đồ đang mặc\nvào Set 2 (Đệ)",
                "Đóng");
    }

    /**
     * Xử lý khi người chơi bấm nút trong menu Set đồ.
     */
    public boolean confirmMenu(Player player, int menuId, int select) {
        if (menuId != MENU_GEAR_PRESET) {
            return false;
        }
        switch (select) {
            case 0 -> equipPreset(player, 1, false);
            case 1 -> equipPreset(player, 2, false);
            case 2 -> savePreset(player, 1, false);
            case 3 -> savePreset(player, 2, false);
            case 4 -> equipPreset(player, 1, true);
            case 5 -> equipPreset(player, 2, true);
            case 6 -> savePreset(player, 1, true);
            case 7 -> savePreset(player, 2, true);
            default -> {}
        }
        return true;
    }

    /**
     * Lưu toàn bộ trang bị đang mặc trên người vào Set chỉ định.
     */
    public void savePreset(Player player, int presetId, boolean isPet) {
        if (player == null) {
            return;
        }
        Player target = isPet ? player.pet : player;
        if (isPet && target == null) {
            Service.gI().sendThongBao(player, "Bạn chưa có đệ tử!");
            return;
        }

        List<Item> body = target.inventory.itemsBody;
        List<PresetItem> items = new ArrayList<>();
        int count = 0;

        for (int i = 0; i < body.size(); i++) {
            Item it = body.get(i);
            if (it != null && it.isNotNullItem()) {
                items.add(new PresetItem(i, it.createTime, it.template.id, it.template.name));
                count++;
            }
        }

        if (count == 0) {
            Service.gI().sendThongBao(player, (isPet ? "Đệ tử" : "Sư phụ") + " hiện không mặc trang bị nào để lưu!");
            return;
        }

        String json = GSON.toJson(items);
        String k = key(player.id, presetId, isPet);
        cache.put(k, items);

        // Lưu vào cơ sở dữ liệu
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "REPLACE INTO player_gear_presets (player_id, preset_id, is_pet, items) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, player.id);
            ps.setInt(2, presetId);
            ps.setInt(3, isPet ? 1 : 0);
            ps.setString(4, json);
            ps.executeUpdate();
        } catch (Exception e) {
            Logger.logException(GearPresetService.class, e, "Lỗi lưu player_gear_presets");
        }

        Service.gI().sendThongBao(player, "Đã lưu " + count + " món đang mặc vào Set " + presetId + " của " + (isPet ? "Đệ tử" : "Sư phụ") + " thành công!");
    }

    /**
     * Mặc toàn bộ các món trong Set chỉ định (tháo đồ cũ ra cất vào hành trang, mặc đồ của Set vào người).
     */
    public void equipPreset(Player player, int presetId, boolean isPet) {
        if (player == null) {
            return;
        }
        Player target = isPet ? player.pet : player;
        if (isPet && target == null) {
            Service.gI().sendThongBao(player, "Bạn chưa có đệ tử!");
            return;
        }

        List<PresetItem> preset = getPreset(player.id, presetId, isPet);
        if (preset == null || preset.isEmpty()) {
            Service.gI().sendThongBao(player, (isPet ? "Đệ tử" : "Sư phụ") + " chưa lưu Set " + presetId + "! Hãy mặc đồ rồi chọn 'Lưu Set " + presetId + "' trước.");
            return;
        }

        int equippedCount = 0;
        List<String> missingItems = new ArrayList<>();

        for (PresetItem pi : preset) {
            // Kiểm tra xem vị trí đó trên người đã mặc đúng món này chưa
            Item currentBodyItem = pi.slot < target.inventory.itemsBody.size() ? target.inventory.itemsBody.get(pi.slot) : null;
            if (currentBodyItem != null && currentBodyItem.isNotNullItem()) {
                if ((pi.createTime > 0 && currentBodyItem.createTime == pi.createTime)
                        || (pi.createTime <= 0 && currentBodyItem.template.id == pi.tempId)) {
                    equippedCount++;
                    continue; // Đã đang mặc đúng món này
                }
            }

            // Tìm món này trong hành trang sư phụ
            int bagIndex = -1;
            // 1. Ưu tiên tìm theo createTime chính xác
            if (pi.createTime > 0) {
                for (int b = 0; b < player.inventory.itemsBag.size(); b++) {
                    Item it = player.inventory.itemsBag.get(b);
                    if (it != null && it.isNotNullItem() && it.createTime == pi.createTime) {
                        bagIndex = b;
                        break;
                    }
                }
            }
            // 2. Dự phòng: tìm theo tempId
            if (bagIndex == -1) {
                for (int b = 0; b < player.inventory.itemsBag.size(); b++) {
                    Item it = player.inventory.itemsBag.get(b);
                    if (it != null && it.isNotNullItem() && it.template.id == pi.tempId) {
                        bagIndex = b;
                        break;
                    }
                }
            }

            if (bagIndex != -1) {
                if (isPet) {
                    InventoryService.gI().itemBagToPetBody(player, bagIndex);
                } else {
                    InventoryService.gI().itemBagToBody(player, bagIndex);
                }
                equippedCount++;
            } else {
                missingItems.add(pi.name != null && !pi.name.isEmpty() ? pi.name : ("Vật phẩm #" + pi.tempId));
            }
        }

        // Cập nhật lại chỉ số và trang phục
        InventoryService.gI().sendItemBags(player);
        InventoryService.gI().sendItemBody(player);
        Service.gI().point(player);
        if (isPet) {
            Service.gI().Send_Caitrang(player.pet);
            Service.gI().showInfoPet(player);
        } else {
            Service.gI().Send_Caitrang(player);
        }
        PlayerDAO.updatePlayer(player);

        if (missingItems.isEmpty()) {
            Service.gI().sendThongBao(player, (isPet ? "Đệ tử" : "Sư phụ") + " đã đổi trọn bộ Set " + presetId + " (" + equippedCount + " món)!");
        } else {
            Service.gI().sendThongBao(player, (isPet ? "Đệ tử" : "Sư phụ") + " đổi Set " + presetId + " (" + equippedCount + " món). Thiếu trong hành trang: " + String.join(", ", missingItems));
        }
    }

    /**
     * Lấy danh sách vật phẩm đã lưu trong preset từ cache hoặc database.
     */
    public List<PresetItem> getPreset(long playerId, int presetId, boolean isPet) {
        String k = key(playerId, presetId, isPet);
        if (cache.containsKey(k)) {
            return cache.get(k);
        }
        initTable();
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT items FROM player_gear_presets WHERE player_id = ? AND preset_id = ? AND is_pet = ?")) {
            ps.setLong(1, playerId);
            ps.setInt(2, presetId);
            ps.setInt(3, isPet ? 1 : 0);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("items");
                    List<PresetItem> list = GSON.fromJson(json, PRESET_LIST_TYPE);
                    if (list != null) {
                        cache.put(k, list);
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            Logger.logException(GearPresetService.class, e, "Lỗi đọc player_gear_presets");
        }
        return null;
    }
}
