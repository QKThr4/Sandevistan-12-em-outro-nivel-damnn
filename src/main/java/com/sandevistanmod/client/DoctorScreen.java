package com.sandevistanmod.client;

import com.sandevistanmod.data.SandevistanData;
import com.sandevistanmod.data.Tx;
import com.sandevistanmod.logic.SandevistanLogic;
import com.sandevistanmod.net.DoctorActionPayload;
import com.sandevistanmod.registry.ModRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/** Tela do Ripperdoc: menu (Loja / Implantes), encaixe do Sandevistan na silhueta e loja de remedios. */
public class DoctorScreen extends Screen {
    private static final int MENU = 0;
    private static final int IMPLANTS = 1;
    private static final int SHOP = 2;

    private int mode = MENU;
    /** O Sandevistan foi encaixado no slot (ainda nao implantado). */
    private boolean placed;
    private Button implantBtn;
    private Button removeBtn;
    private Button buy1;
    private Button buy2;

    public DoctorScreen() {
        super(Tx.t("doc.name"));
    }

    private static int count(net.minecraft.world.item.Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        int n = 0;
        for (ItemStack s : mc.player.getInventory().items) if (s.is(item)) n += s.getCount();
        for (ItemStack s : mc.player.getInventory().offhand) if (s.is(item)) n += s.getCount();
        return n;
    }

    private static boolean creative() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getAbilities().instabuild;
    }

    private static boolean hasImplantItem() {
        return creative() || count(ModRegistries.SANDEVISTAN_ITEM.get()) > 0;
    }

    private int px() {
        return this.width / 2 - 130;
    }

    private int py() {
        return this.height / 2 - 100;
    }

    /** Implantes que cabem no encaixe das costas (no futuro entram outros aqui). */
    public static boolean fitsBack(ItemStack s) {
        return s.is(ModRegistries.SANDEVISTAN_ITEM.get());
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = py();
        implantBtn = null;
        removeBtn = null;
        buy1 = null;
        buy2 = null;
        if (mode == MENU) {
            addRenderableWidget(Button.builder(Tx.t("doc.menu.shop"), b -> setMode(SHOP))
                    .bounds(cx - 80, top + 70, 160, 22).build());
            addRenderableWidget(Button.builder(Tx.t("doc.menu.implants"), b -> setMode(IMPLANTS))
                    .bounds(cx - 80, top + 98, 160, 22).build());
            addRenderableWidget(Button.builder(Tx.t("btn.close"), b -> onClose())
                    .bounds(cx - 80, top + 140, 160, 20).build());
        } else if (mode == IMPLANTS) {
            implantBtn = addRenderableWidget(Button.builder(Tx.t("doc.btn.implant"), b -> {
                        PacketDistributor.sendToServer(new DoctorActionPayload(1));
                        placed = false;
                    })
                    .bounds(cx + 52, top + 56, 74, 20).build());
            removeBtn = addRenderableWidget(Button.builder(Tx.t("doc.btn.remove"), b ->
                            PacketDistributor.sendToServer(new DoctorActionPayload(2)))
                    .bounds(cx + 52, top + 56, 74, 20).build());
            addRenderableWidget(Button.builder(Tx.t("doc.btn.back"), b -> setMode(MENU))
                    .bounds(px() + 6, top + 6, 50, 16).build());
        } else {
            buy1 = addRenderableWidget(Button.builder(Tx.t("doc.btn.buy"), b ->
                            PacketDistributor.sendToServer(new DoctorActionPayload(3)))
                    .bounds(cx + 50, top + 62, 60, 20).build());
            buy2 = addRenderableWidget(Button.builder(Tx.t("doc.btn.buy"), b ->
                            PacketDistributor.sendToServer(new DoctorActionPayload(4)))
                    .bounds(cx + 50, top + 98, 60, 20).build());
            addRenderableWidget(Button.builder(Tx.t("doc.btn.back"), b -> setMode(MENU))
                    .bounds(px() + 6, top + 6, 50, 16).build());
        }
        refresh();
    }

    private void setMode(int m) {
        mode = m;
        carrying = false;
        rebuildWidgets();
    }

    private void refresh() {
        SandevistanData d = ClientData.DATA;
        if (implantBtn != null) {
            implantBtn.visible = !d.implanted && placed;
            implantBtn.active = creative() || count(Items.EMERALD) >= SandevistanLogic.CLINIC_EMERALDS;
        }
        if (removeBtn != null) {
            removeBtn.visible = d.implanted;
            removeBtn.active = !d.active;
        }
        if (buy1 != null) buy1.active = creative() || count(Items.EMERALD) >= SandevistanLogic.STABILIZER_PRICE;
        if (buy2 != null) buy2.active = creative() || count(Items.EMERALD) >= SandevistanLogic.IMMUNO_PRICE;
    }

    @Override
    public void tick() {
        if (placed && !hasImplantItem()) placed = false;
        if (ClientData.DATA.implanted) {
            placed = false;
            carrying = false;
        }
        refresh();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- encaixe das costas e inventario do jogador (arrastar o implante ate o encaixe) ----
    private int slotX() {
        return this.width / 2 - 100;
    }

    private int slotY() {
        return py() + 56;
    }

    private boolean inSlot(double mx, double my) {
        return mx >= slotX() && mx < slotX() + 18 && my >= slotY() && my < slotY() + 18;
    }

    private int invX() {
        return this.width / 2 - 81;
    }

    private int invY() {
        return py() + 132;
    }

    /** Indice do inventario (0 a 8 hotbar, 9 a 35 principal) sob o mouse, ou -1. */
    private int invIndexAt(double mx, double my) {
        int ix = (int) Math.floor((mx - invX()) / 18.0);
        if (ix < 0 || ix > 8 || mx < invX()) return -1;
        double ry = my - invY();
        if (ry >= 0 && ry < 54) return 9 + (int) (ry / 18) * 9 + ix;
        if (ry >= 58 && ry < 76) return ix;
        return -1;
    }

    private ItemStack invStack(int idx) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null || idx < 0 ? ItemStack.EMPTY : mc.player.getInventory().getItem(idx);
    }

    private boolean carrying;
    private double pressX;
    private double pressY;
    private boolean dragged;

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mode == IMPLANTS && button == 0 && !ClientData.DATA.implanted) {
            if (carrying) {
                // segundo clique: encaixa se for no encaixe, senao solta
                if (inSlot(mx, my)) placed = true;
                carrying = false;
                refresh();
                return true;
            }
            if (inSlot(mx, my) && placed) {
                placed = false;
                refresh();
                return true;
            }
            int idx = invIndexAt(mx, my);
            if (idx >= 0 && fitsBack(invStack(idx)) && !placed) {
                carrying = true;
                pressX = mx;
                pressY = my;
                dragged = false;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (carrying && Math.abs(mx - pressX) + Math.abs(my - pressY) > 4) dragged = true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (mode == IMPLANTS && carrying && button == 0 && dragged) {
            // arrastou e soltou: encaixa se soltou no encaixe
            if (inSlot(mx, my)) placed = true;
            carrying = false;
            refresh();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(gg, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int x0 = px();
        int top = py();
        int h = mode == IMPLANTS ? 222 : 200;
        gg.fill(x0, top, x0 + 260, top + h, 0xD0101214);
        gg.fill(x0, top, x0 + 260, top + 2, 0xFFFF2040);
        gg.drawCenteredString(this.font, Tx.t("doc.name"), cx, top + 8, 0xFF5566);

        if (mode == MENU) {
            gg.drawCenteredString(this.font, Tx.t("doc.menu.title"), cx, top + 40, 0xDDDDDD);
        } else if (mode == IMPLANTS) {
            drawImplants(gg, cx, top, mouseX, mouseY);
        } else {
            drawShop(gg, cx, top);
        }
    }

    private void drawImplants(GuiGraphics gg, int cx, int top, int mouseX, int mouseY) {
        SandevistanData d = ClientData.DATA;
        Minecraft mc = Minecraft.getInstance();
        // o proprio jogador (o mesmo modelo do inventario)
        if (mc.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(gg, cx - 40, top + 24, cx + 40, top + 112, 38, 0.0625f,
                    mouseX, mouseY, mc.player);
        }
        int slx = slotX();
        int sly = slotY();
        boolean hot = carrying && inSlot(mouseX, mouseY);
        gg.fill(slx - 1, sly - 1, slx + 19, sly + 19, d.implanted || placed || hot ? 0xFF39FF14 : 0xFF777777);
        gg.fill(slx, sly, slx + 18, sly + 18, 0xFF1A1E22);
        if (d.implanted || placed) {
            gg.renderItem(new ItemStack(ModRegistries.SANDEVISTAN_ITEM.get()), slx + 1, sly + 1);
        }
        gg.drawCenteredString(this.font, Tx.t("slot.back"), slx + 9, sly + 22, 0xAAAAAA);

        if (d.implanted) {
            gg.drawCenteredString(this.font, Tx.t("screen.implanted"), cx, top + 116, 0x55FF55);
        } else if (!placed) {
            gg.drawCenteredString(this.font, Tx.t("doc.place"), cx, top + 116, 0xDDDDDD);
        } else {
            gg.drawCenteredString(this.font, Tx.t("doc.hurt", SandevistanLogic.IMPLANT_HEARTS), cx, top + 116, 0xFF8888);
        }
        if (!d.implanted && placed) {
            gg.renderItem(new ItemStack(Items.EMERALD), cx + 56, top + 80);
            boolean ok = creative() || count(Items.EMERALD) >= SandevistanLogic.CLINIC_EMERALDS;
            gg.drawString(this.font, "x" + SandevistanLogic.CLINIC_EMERALDS, cx + 76, top + 85, ok ? 0x55FF55 : 0xFF5555);
        }
        // inventario do jogador: 3 linhas + hotbar
        int ix = invX();
        int iy = invY();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) drawInvSlot(gg, ix + c * 18, iy + r * 18, 9 + r * 9 + c);
        }
        for (int c = 0; c < 9; c++) drawInvSlot(gg, ix + c * 18, iy + 58, c);
    }

    private void drawInvSlot(GuiGraphics gg, int x, int y, int idx) {
        ItemStack st = invStack(idx);
        boolean glow = fitsBack(st) && !placed && !ClientData.DATA.implanted;
        gg.fill(x, y, x + 18, y + 18, glow ? 0xFF39FF14 : 0xFF3A3F44);
        gg.fill(x + 1, y + 1, x + 17, y + 17, 0xFF1A1E22);
        if (!st.isEmpty()) {
            gg.renderItem(st, x + 1, y + 1);
            gg.renderItemDecorations(this.font, st, x + 1, y + 1);
        }
    }

    private void drawShop(GuiGraphics gg, int cx, int top) {
        int have = count(Items.EMERALD);
        gg.renderItem(new ItemStack(Items.EMERALD), cx - 120, top + 28);
        gg.drawString(this.font, Tx.t("doc.shop.have", have), cx - 98, top + 33, 0x55FF55);
        shopRow(gg, cx, top + 60, new ItemStack(ModRegistries.ESTABILIZADOR.get()), SandevistanLogic.STABILIZER_PRICE);
        shopRow(gg, cx, top + 96, new ItemStack(ModRegistries.IMUNOSSUPRESSOR.get()), SandevistanLogic.IMMUNO_PRICE);
    }

    private void shopRow(GuiGraphics gg, int cx, int y, ItemStack stack, int price) {
        gg.fill(cx - 124, y - 4, cx + 124, y + 28, 0x60000000);
        gg.renderItem(stack, cx - 118, y + 2);
        gg.drawString(this.font, stack.getHoverName(), cx - 96, y + 2, 0xFFFFFF);
        gg.renderItem(new ItemStack(Items.EMERALD), cx - 96, y + 12);
        gg.drawString(this.font, "x" + price, cx - 77, y + 17, count(Items.EMERALD) >= price || creative() ? 0x55FF55 : 0xFF5555);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        super.render(gg, mouseX, mouseY, partialTick);
        if (mode != IMPLANTS) return;
        if (carrying) {
            gg.renderItem(new ItemStack(ModRegistries.SANDEVISTAN_ITEM.get()), mouseX - 8, mouseY - 8);
            return;
        }
        // informacoes do item: so aparecem com o implante no encaixe (ou ao passar o mouse num item do inventario)
        if (inSlot(mouseX, mouseY) && (placed || ClientData.DATA.implanted)) {
            gg.renderTooltip(this.font, new ItemStack(ModRegistries.SANDEVISTAN_ITEM.get()), mouseX, mouseY);
            return;
        }
        int idx = invIndexAt(mouseX, mouseY);
        ItemStack st = invStack(idx);
        if (!st.isEmpty()) gg.renderTooltip(this.font, st, mouseX, mouseY);
    }
}
