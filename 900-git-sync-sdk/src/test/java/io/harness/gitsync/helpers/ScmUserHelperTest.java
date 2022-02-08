package io.harness.gitsync.helpers;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.manage.GlobalContextManager;
import io.harness.rule.Owner;
import io.harness.security.PrincipalContextData;
import io.harness.security.SourcePrincipalContextData;
import io.harness.security.dto.UserPrincipal;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.BHAVYA;
import static org.assertj.core.api.Assertions.assertThat;

@OwnedBy(PL)
public class ScmUserHelperTest extends CategoryTest {
    private final String accountId = "accountId";
    private final String email = "email";
    private final String userName = "userName";
    private final String name = "name";


    @Test
    @Owner(developers = BHAVYA)
    @Category(UnitTests.class)
    public void testGetCurrentUser() {
        try (GlobalContextManager.GlobalContextGuard guard = GlobalContextManager.ensureGlobalContextGuard()) {

            UserPrincipal userPrincipal =
                    new UserPrincipal(name, email, userName, accountId);
            SourcePrincipalContextData principalContextData = SourcePrincipalContextData.builder().principal(userPrincipal).build();
            GlobalContextManager.upsertGlobalContextRecord(principalContextData);
            final EmbeddedUser currentUser = ScmUserHelper.getCurrentUser();
            assertThat(currentUser.getEmail()).isEqualTo(email);
            assertThat(currentUser.getName()).isEqualTo(userName);
        }
    }

    @Test
    @Owner(developers = BHAVYA)
    @Category(UnitTests.class)
    public void testGetCurrentUser_WhenSourcePrincipalIsNotSet() {
        try (GlobalContextManager.GlobalContextGuard guard = GlobalContextManager.ensureGlobalContextGuard()) {

            UserPrincipal userPrincipal =
                    new UserPrincipal(name, email, userName, accountId);
            PrincipalContextData principalContextData = PrincipalContextData.builder().principal(userPrincipal).build();
            GlobalContextManager.upsertGlobalContextRecord(principalContextData);
            final EmbeddedUser currentUser = ScmUserHelper.getCurrentUser();
            assertThat(currentUser.getEmail()).isEqualTo(GitSyncConstants.DEFAULT_USER_EMAIL_ID);
            assertThat(currentUser.getName()).isEqualTo(GitSyncConstants.DEFAULT_USER_NAME);
        }
    }
}
