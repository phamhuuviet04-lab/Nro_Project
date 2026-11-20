package nro.models.server;

import nro.models.services.PlayerService;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.consts.ConstNpc;
import nro.models.managers.GiftCodeManager;
import nro.models.item.Item;
import java.util.ArrayList;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.network.SessionManager;
import nro.models.services.ItemService;
import nro.models.services.PetService;
import nro.models.services.Service;
import nro.models.services_func.Input;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.NpcService;
import nro.models.services.InventoryService;
import nro.models.utils.SystemMetrics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import nro.models.Bot.BotAttackplayer;
import nro.models.Bot.BotManager;
import nro.models.consts.ConstPlayer;
import nro.models.data.LocalManager;
import nro.models.services.TaskService;

/**
 *
 * @author Việt
 *
 */
public class Command {

    private static Command instance;

    private final Map<String, Consumer<Player>> adminCommands = new HashMap<>();
    private final Map<String, BiConsumer<Player, String>> parameterizedCommands = new HashMap<>();

    public static Command gI() {
        if (instance == null) {
            instance = new Command();
        }
        return instance;
    }

    private Command() {
        initAdminCommands();
        initParameterizedCommands();
    }

    private void initAdminCommands() {
        adminCommands.put("item", player -> Input.gI().createFormGiveItem(player));
        adminCommands.put("brl", player -> BrolyManager.gI().showListBoss(player));
        adminCommands.put("getitem", player -> Input.gI().createFormGetItem(player));
        adminCommands.put("chottopql", player -> Service.gI().sendThongBao(player, "Đã chốt top Quy Lão: "
                + nro.models.services.Event2026Service.gI().closeRace(player) + " người nhận quà"));
        adminCommands.put("chottophdba", player -> Service.gI().sendThongBao(player, "Đã chốt top Hộp đen bí ẩn: "
                + nro.models.services.Event2026Service.gI().closeHopDenRace() + " người nhận quà"));
        adminCommands.put("hs", player -> Service.gI().releaseCooldownSkill(player));
        adminCommands.put("d", player -> Service.gI().setPos(player, player.location.x, player.location.y + 10));
        adminCommands.put("a", player -> NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_ADMIN, -1,
                "|0|Time start: " + ServerManager.timeStart
                + "\nClients: " + Client.gI().getPlayers().size()
                + "\n Sessions: " + SessionManager.gI().getNumSession()
                + "\nThreads: " + Thread.activeCount()
                + " luồng" + "\n" + SystemMetrics.ToString(),
                "Ngọc rồng", "Đệ tử", "Bảo trì", "Tìm kiếm\nngười chơi", "Boss", "Công cụ\nadmin", "Đóng"));
    }

    private void initParameterizedCommands() {
        parameterizedCommands.put("m", (player, text) -> {
            try {
                int mapId = Integer.parseInt(text.replace("m", "").trim());
                ChangeMapService.gI().changeMapInYard(player, mapId, -1, -1);
            } catch (NumberFormatException e) {
                Service.gI().sendThongBao(player, "Sai định dạng map ID!");
            }
        });

        parameterizedCommands.put("sukien", (player, text) -> {
            String r = nro.models.services.SeasonEventService.gI().admin(text.substring("sukien".length()));
            NpcService.gI().createTutorial(player, -1, r);
        });

        parameterizedCommands.put("toado", (player, text) -> {
            Service.gI().sendThongBaoOK(player, "x: " + player.location.x + " - y: " + player.location.y);
        });

        parameterizedCommands.put("1", (player, text) -> {
            NpcService.gI().createMenuConMeo(player, 206783, 206783, "|7| Menu bot\n"
                    + "Player online : " + Client.gI().getPlayers().size() + "\n"
                    + "\b|1|Thread: " + Thread.activeCount() + "\n"
                    + "\n Sessions: " + SessionManager.gI().getNumSession() + "\n"
                    + "Bot online : " + BotManager.gI().bot.size(),
                    "Bot\nPem Quái", "Bot\nBán Item", "Bot\nSăn Boss", "Bot\nAttack Player");
            return;
        });

        parameterizedCommands.put("2", (player, text) -> {
            player.originalName = player.name;
            PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_ALL);
            player.originalName = player.name;
            Service.gI().Send_Caitrang(player);
            BotAttackplayer bot = new BotAttackplayer((short) 1624, (short) 1628, (short) 1629, 1, "đánh nhau không?", (short) 0);
            bot.player = player;
            bot.zone = player.zone;
            bot.location.x = player.location.x;
            bot.location.y = player.location.y;

            player.zone.addPlayer(bot);
            BotManager.gI().bot.add(bot);

            for (Player p : player.zone.getPlayers()) {
                if (p.session != null) {
                    Service.gI().sendAppear(bot, p);
                    Service.gI().sendInfoCharMoiToMe(p, bot);
                }
            }

            if (player.session != null) {
                Service.gI().Send_Info_NV(player);
            }

            bot.update();

            ServerNotify.gI().notify("Đã gọi bot tấn công người chơi!");
        });

        parameterizedCommands.put("b", (player, text) -> {
            Input.gI().createFormSenditem1(player);
        });

        parameterizedCommands.put("n", (player, text) -> {
            try {
                int idTask = Integer.parseInt(text.replaceAll("n", "").trim());
                player.playerTask.taskMain.id = idTask - 1;
                player.playerTask.taskMain.index = 0;
                TaskService.gI().sendNextTaskMain(player);
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Sai định dạng task ID!");
            }
        });
        // dmg <value>
parameterizedCommands.put("dm", (player, text) -> {
    try {
        int dmg = Integer.parseInt(text.replace("dm", "").trim());
        player.nPoint.dameg = dmg;
        Service.gI().point(player);
        Service.gI().sendThongBao(player, "SET DAMAGE = " + dmg);
    } catch (Exception e) {
        Service.gI().sendThongBao(player, "Sai cú pháp: dmg <số>");
    }
});

// hpg <value>
parameterizedCommands.put("hp", (player, text) -> {
    try {
        int hpg = Integer.parseInt(text.replace("hp", "").trim());
        player.nPoint.hpg = hpg;
        Service.gI().point(player);
        Service.gI().sendThongBao(player, "SET HP GỐC = " + hpg);
    } catch (Exception e) {
        Service.gI().sendThongBao(player, "Sai cú pháp: hpg <số>");
    }
});

// ki <value>
parameterizedCommands.put("ki", (player, text) -> {
    try {
        int ki = Integer.parseInt(text.replace("ki", "").trim());
        player.nPoint.mpg = ki;
        Service.gI().point(player);
        Service.gI().sendThongBao(player, "SET KI GỐC = " + ki);
    } catch (Exception e) {
        Service.gI().sendThongBao(player, "Sai cú pháp: ki <số>");
    }
});
// up <power>
parameterizedCommands.put("up", (player, text) -> {
    try {
        long power = Long.parseLong(text.replace("up", "").trim());
        Service.gI().addSMTN(player, (byte) 2, power, false);
        Service.gI().sendThongBao(player, "UP SMTN = " + power);
    } catch (Exception e) {
        Service.gI().sendThongBao(player, "Sai cú pháp: up <số>");
    }
});
parameterizedCommands.put("upp", (player, text) -> {
    try {
        if (player.pet == null) {
            Service.gI().sendThongBao(player, "Bạn chưa có đệ tử");
            return;
        }

        long power = Long.parseLong(text.replace("upp", "").trim());
        Service.gI().addSMTN(player.pet, (byte) 2, power, false);
        Service.gI().sendThongBao(
            player,
            "UP TNSM cho đệ tử = " + power
        );
    } catch (Exception e) {
        Service.gI().sendThongBao(player, "Sai cú pháp: upp <số>");
    }
});


        parameterizedCommands.put("i ", (player, text) -> {
            try {
                String[] split = text.split(" ");
                if (split.length < 2) {
                    Service.gI().sendThongBao(player, "Cú pháp: i <itemId> <số lượng> [option:value...]");
                    return;
                }

                int itemId = Integer.parseInt(split[1]);
                int quantity = split.length >= 3 ? Integer.parseInt(split[2]) : 1;

                List<Item.ItemOption> customOptions = new ArrayList<>();
                for (int i = 3; i < split.length; i++) {
                    if (split[i].contains(":")) {
                        String[] optSplit = split[i].split(":");
                        int optionId = Integer.parseInt(optSplit[0]);
                        int optionValue = Integer.parseInt(optSplit[1]);
                        customOptions.add(new Item.ItemOption(optionId, optionValue));
                    }
                }

                for (int i = 0; i < quantity; i++) {
                    Item item = ItemService.gI().createNewItem((short) itemId);
                    if (!customOptions.isEmpty()) {
                        item.itemOptions = new ArrayList<>(customOptions);
                    } else {
                        List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) itemId);
                        if (!ops.isEmpty()) {
                            item.itemOptions = ops;
                        }
                    }
                    InventoryService.gI().addItemBag(player, item);
                }

                InventoryService.gI().sendItemBags(player);
                Service.gI().sendThongBao(player, "GET " + quantity + " x " + ItemService.gI().getTemplate(itemId).name + " [" + itemId + "] SUCCESS!");

            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Lỗi cú pháp! Dùng: i <itemId> <số lượng> [optionId:value]");
            }
        });

        parameterizedCommands.put("setdo", (player, text) -> {
            try {
                String[] parts = text.trim().split("\\s+");
                int planet = player.gender;
                String tier = "tl";
                String stat = "sd";

                if (parts.length == 1) {
                    Service.gI().sendThongBaoOK(player, "|7|CÚ PHÁP LỆNH NHẬN SET ĐỒ 7 SAO:\n"
                            + "|1|setdo [hành_tinh] <loại_đồ> <dòng_sao>\n"
                            + "|2|- Hành tinh: td, nm, xd (hoặc bỏ trống = theo phái)\n"
                            + "- Loại đồ thường:\n"
                            + "  • tl : Đồ Thần Linh (Không SKH)\n"
                            + "  • shop : Đồ VIP Shop (Calic / Zealot / Lưỡng Long)\n"
                            + "  • cui : Đồ Cùi Shop (Cấp 1 - Không SKH)\n"
                            + "- SKH Đồ Cùi (Sơ sinh mặc ngay):\n"
                            + "  • skhcui1 / skhcui2 / skhcui3 (hoặc skh1/2/3)\n"
                            + "- SKH Đồ Thần Linh (Yêu cầu 15-18 tỉ SM):\n"
                            + "  • skhtl1 / skhtl2 / skhtl3 (hoặc skhthan1/2/3)\n"
                            + "- Dòng sao: sd (Sức đánh), hp (Máu), tn (Tiềm năng)\n\n"
                            + "|4|Ví dụ:\n"
                            + "• setdo skhcui1 sd : SKH 1 Đồ Cùi SĐ 7★ (sơ sinh mặc ngay)\n"
                            + "• setdo skhtl1 sd : SKH 1 Đồ Thần SĐ 7★ (chỉ số khủng)\n"
                            + "• setdo tl sd : Set Thần Linh chuẩn SĐ 7★\n"
                            + "• setdo shop hp : Set VIP Shop HP 7★");
                    return;
                } else if (parts.length == 2) {
                    stat = parts[1].toLowerCase();
                } else if (parts.length == 3) {
                    String p1 = parts[1].toLowerCase();
                    String p2 = parts[2].toLowerCase();
                    if (p1.equals("td") || p1.equals("nm") || p1.equals("xd") || p1.equals("0") || p1.equals("1") || p1.equals("2")) {
                        planet = p1.equals("nm") || p1.equals("1") ? 1 : (p1.equals("xd") || p1.equals("2") ? 2 : 0);
                        tier = p2;
                    } else {
                        tier = p1;
                        stat = p2;
                    }
                } else if (parts.length >= 4) {
                    String p1 = parts[1].toLowerCase();
                    planet = p1.equals("nm") || p1.equals("1") ? 1 : (p1.equals("xd") || p1.equals("2") ? 2 : 0);
                    tier = parts[2].toLowerCase();
                    stat = parts[3].toLowerCase();
                }

                if (planet < 0 || planet > 2) planet = 0;

                int[][] SET_SHOP = {
                    {233, 245, 257, 269, 281}, // Trái Đất: Calic
                    {237, 249, 261, 273, 281}, // Namếc: Zealot
                    {241, 253, 265, 277, 281}  // Xayda: Lưỡng Long
                };
                int[][] SET_CUI = {
                    {0, 6, 21, 27, 12}, // Trái Đất: Áo vải 3 lỗ, Quần vải đen, Găng vải đen, Giày nhựa, Rada 1
                    {1, 7, 22, 28, 12}, // Namếc: Áo sợi len, Quần sợi len, Găng sợi len, Giầy sợi len, Rada 1
                    {2, 8, 23, 29, 12}  // Xayda: Áo vải thô, Quần vải thô, Găng vải thô, Giày vải thô, Rada 1
                };
                int[][] SET_THAN = {
                    {555, 556, 562, 563, 561}, // Trái Đất
                    {557, 558, 564, 565, 561}, // Namếc
                    {559, 560, 566, 567, 561}  // Xayda
                };

                int[] itemIds;
                String tierName;
                int skhId = -1;

                if ("shop".equals(tier) || "vip".equals(tier)) {
                    tierName = "VIP Shop";
                    itemIds = SET_SHOP[planet];
                } else if ("cui".equals(tier) || "shopcui".equals(tier)) {
                    tierName = "Shop Cùi (Cấp 1)";
                    itemIds = SET_CUI[planet];
                } else if (tier.startsWith("skh")) {
                    boolean isThan = tier.contains("than") || tier.contains("tl");
                    itemIds = isThan ? SET_THAN[planet] : SET_CUI[planet];

                    int[][] SKH_IDS = {
                        {129, 127, 128}, // Trái Đất: Songoku (129), Thiên Xin Hăng (127), Kirin (128)
                        {130, 131, 132}, // Namếc: Picolo (130), Ốc Tiêu (131), Pikkoro Daimao (132)
                        {133, 134, 135}  // Xayda: Kakarot (133), Cadic (134), Nappa (135)
                    };
                    String[][] SKH_NAMES = {
                        {"Songoku", "Thiên Xin Hăng", "Kirin"},
                        {"Picolo", "Ốc Tiêu", "Pikkoro Daimao"},
                        {"Kakarot", "Cadic", "Nappa"}
                    };
                    int skhIdx = 0;
                    if (tier.contains("2") || tier.contains("txh") || tier.contains("octieu") || tier.contains("cadic")) {
                        skhIdx = 1;
                    } else if (tier.contains("3") || tier.contains("kirin") || tier.contains("pikkoro") || tier.contains("nappa")) {
                        skhIdx = 2;
                    }
                    skhId = SKH_IDS[planet][skhIdx];
                    tierName = "SKH " + SKH_NAMES[planet][skhIdx] + (isThan ? " (Đồ Thần)" : " (Đồ Cùi)");
                } else {
                    tierName = "Thần Linh";
                    itemIds = SET_THAN[planet];
                }

                int statOptId = 50;
                int statParam = 21;
                String statName = "Sức đánh +21%";
                if ("hp".equals(stat)) {
                    statOptId = 77;
                    statParam = 35;
                    statName = "HP +35%";
                } else if ("tn".equals(stat) || "tiemnang".equals(stat)) {
                    statOptId = 101;
                    statParam = 35;
                    statName = "Tiềm năng +35%";
                }

                int emptySlots = 0;
                for (Item slot : player.inventory.itemsBag) {
                    if (!slot.isNotNullItem()) {
                        emptySlots++;
                    }
                }
                if (emptySlots < itemIds.length) {
                    Service.gI().sendThongBao(player, "Hành trang cần ít nhất " + itemIds.length + " ô trống (hiện còn " + emptySlots + ")");
                    return;
                }

                for (int itemId : itemIds) {
                    Item it = ItemService.gI().createNewItem((short) itemId, 1);
                    nro.models.services.RewardService.gI().initChiSoItem(it);
                    if (it.itemOptions.isEmpty()) {
                        List<Item.ItemOption> shopOps = ItemService.gI().getListOptionItemShop((short) itemId);
                        if (shopOps != null && !shopOps.isEmpty()) {
                            it.itemOptions.addAll(shopOps);
                        }
                    }
                    if (skhId > 0) {
                        it.itemOptions.add(new Item.ItemOption(skhId, 1));
                        for (int subId : ItemService.gI().getOptionIdsBySKH(skhId)) {
                            it.itemOptions.add(new Item.ItemOption(subId, 1));
                        }
                    }
                    it.itemOptions.add(new Item.ItemOption(107, 7));
                    it.itemOptions.add(new Item.ItemOption(102, 7));
                    it.itemOptions.add(new Item.ItemOption(statOptId, statParam));
                    InventoryService.gI().addItemBag(player, it);
                }

                InventoryService.gI().sendItemBags(player);
                Service.gI().sendThongBao(player, "Đã nhận Set " + tierName + " 7 sao (" + statName + ") thành công!");

            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Lỗi cú pháp: " + e.getMessage());
            }
        });
    }

    public void chat(Player player, String text) {
        String cleanedText = text.trim();
        if (cleanedText.isEmpty()) {
            return;
        }
        if (!check(player, cleanedText)) {
            Service.gI().chat(player, cleanedText);
        }
    }

    public boolean check(Player player, String text) {
        String cmd = text.startsWith("/") ? text.substring(1).trim() : text;
        if (cmd.startsWith("setdo")) {
            if (parameterizedCommands.containsKey("setdo")) {
                parameterizedCommands.get("setdo").accept(player, cmd);
                return true;
            }
        }

        if (player.isAdmin()) {
            if (adminCommands.containsKey(cmd)) {
                adminCommands.get(cmd).accept(player);
                return true;
            }

            for (Map.Entry<String, BiConsumer<Player, String>> entry : parameterizedCommands.entrySet()) {
                if (cmd.startsWith(entry.getKey())) {
                    entry.getValue().accept(player, cmd);
                    return true;
                }
            }
        }

        if (text.equals("chiso") || text.equals("chi so")) {
            nro.models.services.StatsInfoService.gI().show(player, false);
            return true;
        }
        if (text.startsWith("goiboss ") && player.isAdmin()) { // admin: gọi boss theo BossID
            try {
                nro.models.boss.Boss b = nro.models.boss.Boss_Manager.BossManager.gI().createBoss(Integer.parseInt(text.substring(8).trim()));
                Service.gI().sendThongBao(player, b == null ? "Không gọi được boss" : "Đã gọi " + b.name + (b.zone != null ? " tại " + b.zone.map.mapName : ""));
            } catch (Exception e) {
                Service.gI().sendThongBao(player, "Lỗi: " + e.getMessage());
            }
            return true;
        }
        if (text.startsWith("den boss ") && player.isAdmin()) { // admin: dịch chuyển tới boss theo tên
            String want = text.substring(9).trim().toLowerCase();
            java.util.List<nro.models.boss.Boss> all = new java.util.ArrayList<>(nro.models.boss.Boss_Manager.BossManager.gI().getBosses());
            all.addAll(nro.models.boss.Boss_Manager.BrolyManager.gI().getBosses());
            for (nro.models.boss.Boss b : all) {
                if (b.zone != null && !b.isDie() && b.name.toLowerCase().startsWith(want)) {
                    nro.models.map.service.ChangeMapService.gI().changeMap(player, b.zone, b.location.x, b.location.y);
                    Service.gI().sendThongBao(player, "Đến " + b.name + " tại " + b.zone.map.mapName + " khu " + b.zone.zoneId + " HP " + b.nPoint.hpMax);
                    return true;
                }
            }
            Service.gI().sendThongBao(player, "Không thấy boss " + want);
            return true;
        }
        if ((text.equals("roiskh") || text.equals("roiskh cu") || text.equals("roiskh boss")) && player.isAdmin()) { // admin: thả 1 món set kích hoạt như quái / boss rơi (thử thông báo nhặt)
            int[][] sets = {{245, 246, 247, 248}, {237, 238, 239, 240}, {241, 242, 243, 244}};
            int g = Math.min(Math.max(player.gender, 0), 2);
            if (text.equals("roiskh cu")) {
                sets = new int[][]{{127}, {130}, {133}};
            }
            short temp = (short) ItemService.gI().randTempItemKichHoat(player.gender);
            boolean boss = text.equals("roiskh boss");
            if (boss) { // giả như boss Baby vừa chết tại khu này
                player.zone.bossDropName = "Baby";
                player.zone.bossDropTime = System.currentTimeMillis();
            }
            nro.models.map.ItemMap it = new nro.models.map.ItemMap(player.zone, temp, 1, player.location.x, player.zone.map.yPhysicInTop(player.location.x, player.location.y - 24), player.id);
            it.options.addAll(ItemService.gI().getListOptionItemShop(temp));
            for (int id : sets[g]) {
                it.options.add(new nro.models.item.Item.ItemOption(id, 0));
            }
            it.fromMob = !boss;
            Service.gI().dropItemMap(player.zone, it);
            Service.gI().sendThongBao(player, "itemMapId=" + it.itemMapId);
            return true;
        }
        if (text.startsWith("daubot ") && player.isAdmin()) { // admin: "daubot <tên bot> sh|dh" cho bot đi giải Siêu hạng / ĐHVT 23 ngay (đấu thật)
            String[] p = text.substring(7).trim().split("\\s+");
            nro.models.Bot.Bot bot = null;
            for (nro.models.Bot.Bot x : nro.models.Bot.BotWorld.gI().bots()) {
                if (x.name != null && x.name.equalsIgnoreCase(p[0])) {
                    bot = x;
                }
            }
            if (bot == null) {
                Service.gI().sendThongBao(player, "Không thấy bot " + p[0]);
                return true;
            }
            boolean ok = p.length > 1 && p[1].equals("dh") ? nro.models.Bot.BotArena.startDhvt23(bot) : nro.models.Bot.BotArena.startSieuHang(bot);
            Service.gI().sendThongBao(player, bot.name + (ok ? " đã vào trận" : " không vào được trận (thiếu điều kiện)"));
            return true;
        }
        if (text.equals("giet boss") && player.isAdmin()) { // admin: hạ mọi boss trong khu, nhận thưởng
            for (Player p : player.zone.getBossesCopy()) {
                if (p instanceof nro.models.boss.Boss b && !b.isDie()) {
                    Service.gI().sendThongBao(player, "Hạ " + b.name + " (HP " + b.nPoint.hpMax + ")");
                    b.adminKill(player);
                }
            }
            return true;
        }
        if (text.equals("pkall") && player.isAdmin()) { // admin: bật PK để thử chiến đấu
            PlayerService.gI().changeAndSendTypePK(player, ConstPlayer.PK_ALL);
            return true;
        }
        if (text.equals("chisoraw") && player.isAdmin()) {
            // admin: in mọi biến chỉ số (dùng để kiểm tra option có tác dụng hay không)
            StringBuilder sb = new StringBuilder("RAW|");
            for (java.lang.reflect.Field f : nro.models.player.NPoint.class.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    Object v = f.get(player.nPoint);
                    if (v instanceof Number || v instanceof Boolean) {
                        sb.append(f.getName()).append('=').append(v).append(';');
                    } else if (v instanceof java.util.List<?> l) {
                        long sum = 0;
                        for (Object o : l) {
                            if (o instanceof Number n) {
                                sum += n.longValue();
                            }
                        }
                        sb.append(f.getName()).append('=').append(sum).append(';');
                    }
                } catch (Exception ignored) {
                }
            }
            sb.append("speed2=").append(player.nPoint.speed).append(';');
            nro.models.services.Service.gI().sendThongBao(player, sb.toString());
            return true;
        }
        if (text.equals("chiso de") || text.equals("chi so de")) {
            nro.models.services.StatsInfoService.gI().show(player, true);
            return true;
        }

        if (text.equals("thao buoc chan")) {
            nro.models.services.Event2026Service.gI().unequipFoot(player);
            return true;
        }

        if (text.startsWith("ten con la ")) {
            PetService.gI().changeNamePet(player, text.replace("ten con la ", ""));
        }

        if (player.pet != null) {
            switch (text) {
                case "di theo", "follow" ->
                    player.pet.changeStatus(Pet.FOLLOW);
                case "bao ve", "protect" ->
                    player.pet.changeStatus(Pet.PROTECT);
                case "tan cong", "attack" ->
                    player.pet.changeStatus(Pet.ATTACK);
                case "ve nha", "go home" ->
                    player.pet.changeStatus(Pet.GOHOME);
                case "bien hinh" ->
                    player.pet.transform();
                case "sach tuyet ky" -> {
                    int typePet = player.pet.typePet;
                    if (typePet == 2 || typePet == 3 || typePet == 4) {
                        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                            Item item = player.inventory.itemsBag.get(i);
                            if (item != null && item.isNotNullItem() && item.template.type == 25) {
                                if (player.pet.nPoint != null && player.pet.nPoint.power >= 1500000) {
                                    Item old = InventoryService.gI().putItemBody(player.pet, item);
                                    player.inventory.itemsBag.set(i, old);
                                    InventoryService.gI().sendItemBags(player);
                                    InventoryService.gI().sendItemBody(player);
                                    Service.gI().Send_Caitrang(player.pet);
                                    Service.gI().Send_Caitrang(player);
                                    Service.gI().sendThongBao(player, "Đã dùng " + item.template.name + " cho đệ tử");
                                } else {
                                    Service.gI().sendThongBaoOK(player, "Đệ tử cần đạt 1tr5 sức mạnh để trang bị.");
                                }
                                break;
                            }
                        }
                    } else {
                        Service.gI().sendThongBaoOK(player, "Chỉ đệ tử (Goku vô cực, Kid Beerus, Jiren) mới có thể dùng sách tuyệt kỹ.");
                    }
                }
            }
        }
        return false;
    }
}
