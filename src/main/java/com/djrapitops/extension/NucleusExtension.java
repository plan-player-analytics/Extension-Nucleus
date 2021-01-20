/*
    Copyright(c) 2019 Risto Lahtela (Rsl1122)

    The MIT License(MIT)

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files(the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions :
    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/
package com.djrapitops.extension;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.ElementOrder;
import com.djrapitops.plan.extension.FormatType;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import io.github.nucleuspowered.nucleus.api.NucleusAPI;
import io.github.nucleuspowered.nucleus.api.module.home.data.Home;
import io.github.nucleuspowered.nucleus.api.module.jail.data.Jailing;
import io.github.nucleuspowered.nucleus.api.module.kit.NucleusKitService;
import io.github.nucleuspowered.nucleus.api.module.mute.data.Mute;
import io.github.nucleuspowered.nucleus.api.module.note.data.Note;
import io.github.nucleuspowered.nucleus.api.module.warp.NucleusWarpService;
import io.github.nucleuspowered.nucleus.api.module.warp.data.Warp;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * DataExtension for Nucleus.
 * <p>
 * Adapted from PluginData implementation by Vankka.
 *
 * @author Rsl1122
 */
@PluginInfo(name = "Nucleus", iconName = "atom", iconFamily = Family.SOLID, color = Color.BLACK)
@TabInfo(
        tab = "Punishments",
        iconName = "gavel",
        elementOrder = {ElementOrder.VALUES, ElementOrder.TABLE}
)
@TabInfo(
        tab = "Homes",
        iconName = "home",
        elementOrder = {ElementOrder.TABLE}
)
@TabInfo(
        tab = "Server",
        iconName = "box",
        elementOrder = {ElementOrder.TABLE}
)
@TabOrder({"Homes", "Punishments", "Kits"})
@InvalidateMethod("warnings")
public class NucleusExtension implements DataExtension {

    private final UserStorageService userStorageService;

    public NucleusExtension() {
        userStorageService = Sponge.getServiceManager().provide(UserStorageService.class).orElseThrow(IllegalStateException::new);
    }

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{
                CallEvents.SERVER_EXTENSION_REGISTER,
                CallEvents.SERVER_PERIODICAL,
                CallEvents.PLAYER_JOIN,
                CallEvents.PLAYER_LEAVE
        };
    }

    private Optional<User> getUser(UUID playerUUID) {
        Optional<Player> player = Sponge.getServer().getPlayer(playerUUID);
        if (player.isPresent()) {
            return player.map(p -> p); // Maps to User
        } else if (userStorageService != null) {
            return userStorageService.get(playerUUID);
        } else {
            return Optional.empty();
        }
    }

    @BooleanProvider(
            text = "Muted",
            iconName = "bell-slash",
            iconColor = Color.DEEP_ORANGE,
            priority = 100,
            conditionName = "isMuted"
    )
    @Tab("Punishments")
    public boolean isMuted(UUID playerUUID) {
        return getUser(playerUUID)
                .flatMap(user -> NucleusAPI.getMuteService()
                        .map(service -> service.isMuted(user))
                ).orElse(false);
    }

    @StringProvider(
            text = "Operator",
            description = "Who muted the player",
            iconName = "user",
            iconColor = Color.DEEP_ORANGE,
            iconFamily = Family.REGULAR,
            priority = 99,
            playerName = true
    )
    @Conditional("isMuted")
    @Tab("Punishments")
    public String muteOperator(UUID playerUUID) {
        return getUser(playerUUID)
                .flatMap(user -> NucleusAPI.getMuteService()
                        .flatMap(service -> service.getPlayerMuteInfo(user)))
                .flatMap(Mute::getMuter)
                .flatMap(this::getUser)
                .map(User::getName)
                .orElse("Unknown");
    }

    @NumberProvider(
            text = "Date",
            iconName = "calendar",
            iconColor = Color.DEEP_ORANGE,
            iconFamily = Family.REGULAR,
            priority = 98,
            format = FormatType.DATE_SECOND
    )
    @Conditional("isMuted")
    @Tab("Punishments")
    public long muteStart(UUID playerUUID) {
        return getUser(playerUUID)
                .flatMap(user -> NucleusAPI.getMuteService()
                        .flatMap(service -> service.getPlayerMuteInfo(user)))
                .flatMap(Mute::getCreationInstant)
                .map(Instant::toEpochMilli)
                .orElse(-1L);
    }

    @NumberProvider(
            text = "Ends",
            iconName = "calendar-check",
            iconColor = Color.DEEP_ORANGE,
            iconFamily = Family.REGULAR,
            priority = 97,
            format = FormatType.DATE_SECOND
    )
    @Conditional("isMuted")
    @Tab("Punishments")
    public long muteEnd(UUID playerUUID) {
        return getUser(playerUUID)
                .flatMap(user -> NucleusAPI.getMuteService()
                        .flatMap(service -> service.getPlayerMuteInfo(user)))
                .flatMap(Mute::getRemainingTime)
                .map(duration -> duration.plusMillis(System.currentTimeMillis()).toMillis())
                .orElse(-1L);
    }

    @StringProvider(
            text = "Reason",
            description = "Why the player was muted",
            iconName = "comment",
            iconColor = Color.DEEP_ORANGE,
            iconFamily = Family.REGULAR,
            priority = 96
    )
    @Conditional("isMuted")
    @Tab("Punishments")
    public String muteReason(UUID playerUUID) {
        return getUser(playerUUID)
                .flatMap(user -> NucleusAPI.getMuteService()
                        .flatMap(service -> service.getPlayerMuteInfo(user)))
                .map(Mute::getReason)
                .orElse("Unspecified");
    }

    @BooleanProvider(
            text = "Jailed",
            iconName = "bars",
            iconColor = Color.AMBER,
            priority = 90,
            conditionName = "isJailed"
    )
    @Tab("Punishments")
    public boolean isJailed(UUID playerUUID) {
        return getUser(playerUUID)
                .flatMap(user -> NucleusAPI.getJailService()
                        .map(service -> service.isPlayerJailed(user))
                ).orElse(false);
    }

    @StringProvider(
            text = "Operator",
            description = "Who jailed the player",
            iconName = "user",
            iconColor = Color.AMBER,
            iconFamily = Family.REGULAR,
            priority = 89,
            playerName = true
    )
    @Conditional("isJailed")
    @Tab("Punishments")
    public String jailOperator(UUID playerUUID) {
        return getUser(playerUUID)
                .flatMap(user -> NucleusAPI.getJailService()
                        .flatMap(service -> service.getPlayerJailData(user)))
                .flatMap(Jailing::getJailer)
                .flatMap(this::getUser)
                .map(User::getName)
                .orElse("Unknown");
    }

    @NumberProvider(
            text = "Date",
            iconName = "calendar",
            iconColor = Color.AMBER,
            iconFamily = Family.REGULAR,
            priority = 88,
            format = FormatType.DATE_SECOND
    )
    @Conditional("isJailed")
    @Tab("Punishments")
    public long jailStart(UUID playerUUID) {
        return getUser(playerUUID)
                .flatMap(user -> NucleusAPI.getJailService()
                        .flatMap(service -> service.getPlayerJailData(user)))
                .flatMap(Jailing::getCreationInstant)
                .map(Instant::toEpochMilli)
                .orElse(-1L);
    }

    @NumberProvider(
            text = "Ends",
            iconName = "calendar-check",
            iconColor = Color.AMBER,
            iconFamily = Family.REGULAR,
            priority = 87,
            format = FormatType.DATE_SECOND
    )
    @Conditional("isJailed")
    @Tab("Punishments")
    public long jailEnd(UUID playerUUID) {
        return getUser(playerUUID)
                .flatMap(user -> NucleusAPI.getJailService()
                        .flatMap(service -> service.getPlayerJailData(user)))
                .flatMap(Jailing::getRemainingTime)
                .map(duration -> duration.plusMillis(System.currentTimeMillis()).toMillis())
                .orElse(-1L);
    }

    @StringProvider(
            text = "Reason",
            description = "Why the player was jailed",
            iconName = "comment",
            iconColor = Color.AMBER,
            iconFamily = Family.REGULAR,
            priority = 96
    )
    @Conditional("isJailed")
    @Tab("Punishments")
    public String jailReason(UUID playerUUID) {
        return getUser(playerUUID)
                .flatMap(user -> NucleusAPI.getJailService()
                        .flatMap(service -> service.getPlayerJailData(user)))
                .map(Jailing::getReason)
                .orElse("Unspecified");
    }

    @StringProvider(
            text = "Jail",
            description = "Where the player is jailed at",
            iconName = "bars",
            iconColor = Color.AMBER,
            iconFamily = Family.REGULAR,
            priority = 95
    )
    @Conditional("isJailed")
    @Tab("Punishments")
    public String jail(UUID playerUUID) {
        return getUser(playerUUID)
                .flatMap(user -> NucleusAPI.getJailService()
                        .flatMap(service -> service.getPlayerJailData(user)))
                .map(Jailing::getJailName)
                .orElse("-");
    }

    @StringProvider(
            text = "Nickname",
            iconName = "id-badge",
            iconColor = Color.GREEN,
            iconFamily = Family.REGULAR,
            priority = 100,
            showInPlayerTable = true
    )
    public String nickname(UUID playerUUID) {
        return getUser(playerUUID)
                .flatMap(user -> NucleusAPI.getNicknameService()
                        .flatMap(service -> service.getNickname(user)))
                .map(TextSerializers.FORMATTING_CODE::serialize)
                .orElse("-");
    }

    @NumberProvider(
            text = "Homes",
            iconName = "home",
            iconColor = Color.LIGHT_GREEN
    )
    @Tab("Homes")
    public long homeCount(UUID playerUUID) {
        return NucleusAPI.getHomeService().map(service -> service.getHomeCount(playerUUID))
                .orElse(0);
    }

    @TableProvider(tableColor = Color.LIGHT_GREEN)
    @Tab("Homes")
    public Table homes(UUID playerUUID) {
        List<Home> homes = NucleusAPI.getHomeService().map(service -> service.getHomes(playerUUID))
                .orElse(Collections.emptyList());

        Table.Factory table = Table.builder()
                .columnOne("Home", Icon.called("home").build());

        for (Home home : homes) {
            table.addRow(home.getName());
        }

        return table.build();
    }

    @TableProvider(tableColor = Color.LIGHT_BLUE)
    public Table notes(UUID playerUUID) {
        List<Note> notes = getUser(playerUUID)
                .flatMap(user -> NucleusAPI.getNoteService()
                        .map(service -> (List<Note>) service.getNotes(user)))
                .orElse(Collections.emptyList());

        Table.Factory table = Table.builder()
                .columnOne("Noter", Icon.called("pen").of(Family.SOLID).build())
                .columnTwo("Note", Icon.called("sticky-note").of(Family.REGULAR).build());

        for (Note note : notes) {
            String noter = note.getNoter().flatMap(this::getUser).map(User::getName).orElse("Unknown");
            table.addRow(noter, note.getNote());
        }

        return table.build();
    }

    @TableProvider(tableColor = Color.PURPLE)
    @Tab("Server")
    public Table kits() {
        Table.Factory table = Table.builder()
                .columnOne("Kit", Icon.called("box").build());

        NucleusAPI.getKitService().map(NucleusKitService::getKitNames).orElse(Collections.emptySet())
                .stream().sorted()
                .forEach(table::addRow);

        return table.build();
    }

    @TableProvider(tableColor = Color.PURPLE)
    @Tab("Server")
    public Table warps() {
        Table.Factory table = Table.builder()
                .columnOne("Warp", Icon.called("map-marker-alt").build())
                .columnTwo("Description", Icon.called("sticky-note").of(Family.REGULAR).build())
                .columnThree("Category", Icon.called("list").build());

        List<Warp> warps = NucleusAPI.getWarpService().map(NucleusWarpService::getAllWarps).orElse(Collections.emptyList());

        for (Warp warp : warps) {
            table.addRow(
                    warp.getName(),
                    warp.getDescription().map(TextSerializers.FORMATTING_CODE::serialize).orElse(null),
                    warp.getCategory().orElse(null)
            );
        }

        return table.build();
    }
}