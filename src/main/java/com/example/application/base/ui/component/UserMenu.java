package com.example.application.base.ui.component;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class UserMenu extends Composite<MenuBar> {

    public UserMenu(AuthenticationContext authenticationContext) {
        addClassName("user-menu");
        var user = authenticationContext.getAuthenticatedUser(OidcUser.class).orElseThrow();

        var avatar = new Avatar(user.getFullName(), user.getPicture());
        avatar.addThemeVariants(AvatarVariant.LUMO_XSMALL);
        avatar.addClassNames(LumoUtility.Margin.Right.SMALL);

        var userMenu = getContent();
        userMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        var userMenuItem = userMenu.addItem(avatar);
        userMenuItem.add(user.getFullName());
        if (user.getProfile() != null) {
            userMenuItem.getSubMenu().addItem("View Profile",
                    event -> UI.getCurrent().getPage().open(user.getProfile()));
        }
        userMenuItem.getSubMenu().addItem("Logout", event -> authenticationContext.logout());
        addClassNames(LumoUtility.Margin.Horizontal.MEDIUM);
    }
}
