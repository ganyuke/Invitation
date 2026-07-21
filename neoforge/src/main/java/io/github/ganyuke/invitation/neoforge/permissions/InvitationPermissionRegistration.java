package io.github.ganyuke.invitation.neoforge.permissions;

import io.github.ganyuke.invitation.neoforge.InvitationNeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;

@EventBusSubscriber(modid = InvitationNeoForge.MOD_ID)
public final class InvitationPermissionRegistration {
    private InvitationPermissionRegistration() {
    }

    @SubscribeEvent
    public static void onPermissionGather(PermissionGatherEvent.Nodes event) {
        event.addNodes(
                InvitationPermissionNodes.INVITE_USE,
                InvitationPermissionNodes.INVITE_LOG
        );
    }
}
