package cn.iocoder.yudao.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleMenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.mq.producer.permission.PermissionProducer;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.framework.datapermission.core.dept.service.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.enums.DataScopeEnum;
import cn.iocoder.yudao.module.system.test.BaseDbUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(PermissionServiceImpl.class)
public class PermissionServiceTest extends BaseDbUnitTest {

    @Resource
    private PermissionServiceImpl permissionService;

    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private UserRoleMapper userRoleMapper;

    @MockBean
    private RoleService roleService;
    @MockBean
    private MenuService menuService;
    @MockBean
    private DeptService deptService;

    @MockBean
    private PermissionProducer permissionProducer;

    @Test
    public void testProcessRoleDeleted() {
        // ????????????
        Long roleId = randomLongId();
        // mock ?????? UserRole
        UserRoleDO userRoleDO01 = randomPojo(UserRoleDO.class, o -> o.setRoleId(roleId)); // ?????????
        userRoleMapper.insert(userRoleDO01);
        UserRoleDO userRoleDO02 = randomPojo(UserRoleDO.class); // ????????????
        userRoleMapper.insert(userRoleDO02);
        // mock ?????? RoleMenu
        RoleMenuDO roleMenuDO01 = randomPojo(RoleMenuDO.class, o -> o.setRoleId(roleId)); // ?????????
        roleMenuMapper.insert(roleMenuDO01);
        RoleMenuDO roleMenuDO02 = randomPojo(RoleMenuDO.class); // ????????????
        roleMenuMapper.insert(roleMenuDO02);

        // ??????
        permissionService.processRoleDeleted(roleId);
        // ???????????? RoleMenuDO
        List<RoleMenuDO> dbRoleMenus = roleMenuMapper.selectList();
        assertEquals(1, dbRoleMenus.size());
        assertPojoEquals(dbRoleMenus.get(0), roleMenuDO02);
        // ???????????? UserRoleDO
        List<UserRoleDO> dbUserRoles = userRoleMapper.selectList();
        assertEquals(1, dbUserRoles.size());
        assertPojoEquals(dbUserRoles.get(0), userRoleDO02);
        // ????????????
        verify(permissionProducer).sendRoleMenuRefreshMessage();
    }

    @Test
    public void testProcessMenuDeleted() {
        // ????????????
        Long menuId = randomLongId();
        // mock ??????
        RoleMenuDO roleMenuDO01 = randomPojo(RoleMenuDO.class, o -> o.setMenuId(menuId)); // ?????????
        roleMenuMapper.insert(roleMenuDO01);
        RoleMenuDO roleMenuDO02 = randomPojo(RoleMenuDO.class); // ????????????
        roleMenuMapper.insert(roleMenuDO02);

        // ??????
        permissionService.processMenuDeleted(menuId);
        // ????????????
        List<RoleMenuDO> dbRoleMenus = roleMenuMapper.selectList();
        assertEquals(1, dbRoleMenus.size());
        assertPojoEquals(dbRoleMenus.get(0), roleMenuDO02);
        // ????????????
        verify(permissionProducer).sendRoleMenuRefreshMessage();
    }

    @Test
    public void testProcessUserDeleted() {
        // ????????????
        Long userId = randomLongId();
        // mock ??????
        UserRoleDO userRoleDO01 = randomPojo(UserRoleDO.class, o -> o.setUserId(userId)); // ?????????
        userRoleMapper.insert(userRoleDO01);
        UserRoleDO userRoleDO02 = randomPojo(UserRoleDO.class); // ????????????
        userRoleMapper.insert(userRoleDO02);

        // ??????
        permissionService.processUserDeleted(userId);
        // ????????????
        List<UserRoleDO> dbUserRoles = userRoleMapper.selectList();
        assertEquals(1, dbUserRoles.size());
        assertPojoEquals(dbUserRoles.get(0), userRoleDO02);
    }

    @Test // ????????? context ???????????????
    public void testGetDeptDataPermission_fromContext() {
        // ????????????
        LoginUser loginUser = randomPojo(LoginUser.class);
        // mock ??????
        DeptDataPermissionRespDTO respDTO = new DeptDataPermissionRespDTO();
        loginUser.setContext(PermissionServiceImpl.CONTEXT_KEY, respDTO);

        // ??????
        DeptDataPermissionRespDTO result = permissionService.getDeptDataPermission(loginUser);
        // ??????
        assertSame(respDTO, result);
    }

    @Test
    public void testGetDeptDataPermission_All() {
        // ????????????
        LoginUser loginUser = randomPojo(LoginUser.class);
        // mock ??????
        RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setDataScope(DataScopeEnum.ALL.getScope()));
        when(roleService.getRolesFromCache(same(loginUser.getRoleIds()))).thenReturn(singletonList(roleDO));

        // ??????
        DeptDataPermissionRespDTO result = permissionService.getDeptDataPermission(loginUser);
        // ??????
        assertTrue(result.getAll());
        assertFalse(result.getSelf());
        assertTrue(CollUtil.isEmpty(result.getDeptIds()));
        assertSame(result, loginUser.getContext(PermissionServiceImpl.CONTEXT_KEY, DeptDataPermissionRespDTO.class));
    }

    @Test
    public void testGetDeptDataPermission_DeptCustom() {
        // ????????????
        LoginUser loginUser = randomPojo(LoginUser.class);
        // mock ??????
        RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setDataScope(DataScopeEnum.DEPT_CUSTOM.getScope()));
        when(roleService.getRolesFromCache(same(loginUser.getRoleIds()))).thenReturn(singletonList(roleDO));

        // ??????
        DeptDataPermissionRespDTO result = permissionService.getDeptDataPermission(loginUser);
        // ??????
        assertFalse(result.getAll());
        assertFalse(result.getSelf());
        assertEquals(roleDO.getDataScopeDeptIds().size() + 1, result.getDeptIds().size());
        assertTrue(CollUtil.containsAll(result.getDeptIds(), roleDO.getDataScopeDeptIds()));
        assertTrue(CollUtil.contains(result.getDeptIds(), loginUser.getDeptId()));
        assertSame(result, loginUser.getContext(PermissionServiceImpl.CONTEXT_KEY, DeptDataPermissionRespDTO.class));
    }

    @Test
    public void testGetDeptDataPermission_DeptOnly() {
        // ????????????
        LoginUser loginUser = randomPojo(LoginUser.class);
        // mock ??????
        RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setDataScope(DataScopeEnum.DEPT_ONLY.getScope()));
        when(roleService.getRolesFromCache(same(loginUser.getRoleIds()))).thenReturn(singletonList(roleDO));

        // ??????
        DeptDataPermissionRespDTO result = permissionService.getDeptDataPermission(loginUser);
        // ??????
        assertFalse(result.getAll());
        assertFalse(result.getSelf());
        assertEquals(1, result.getDeptIds().size());
        assertTrue(CollUtil.contains(result.getDeptIds(), loginUser.getDeptId()));
        assertSame(result, loginUser.getContext(PermissionServiceImpl.CONTEXT_KEY, DeptDataPermissionRespDTO.class));
    }

    @Test
    public void testGetDeptDataPermission_DeptAndChild() {
        // ????????????
        LoginUser loginUser = randomPojo(LoginUser.class);
        // mock ??????????????????
        RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setDataScope(DataScopeEnum.DEPT_AND_CHILD.getScope()));
        when(roleService.getRolesFromCache(same(loginUser.getRoleIds()))).thenReturn(singletonList(roleDO));
        // mock ??????????????????
        DeptDO deptDO = randomPojo(DeptDO.class);
        when(deptService.getDeptsByParentIdFromCache(eq(loginUser.getDeptId()), eq(true)))
                .thenReturn(singletonList(deptDO));

        // ??????
        DeptDataPermissionRespDTO result = permissionService.getDeptDataPermission(loginUser);
        // ??????
        assertFalse(result.getAll());
        assertFalse(result.getSelf());
        assertEquals(1, result.getDeptIds().size());
        assertTrue(CollUtil.contains(result.getDeptIds(), deptDO.getId()));
        assertSame(result, loginUser.getContext(PermissionServiceImpl.CONTEXT_KEY, DeptDataPermissionRespDTO.class));
    }

    @Test
    public void testGetDeptDataPermission_Self() {
        // ????????????
        LoginUser loginUser = randomPojo(LoginUser.class);
        // mock ??????
        RoleDO roleDO = randomPojo(RoleDO.class, o -> o.setDataScope(DataScopeEnum.SELF.getScope()));
        when(roleService.getRolesFromCache(same(loginUser.getRoleIds()))).thenReturn(singletonList(roleDO));

        // ??????
        DeptDataPermissionRespDTO result = permissionService.getDeptDataPermission(loginUser);
        // ??????
        assertFalse(result.getAll());
        assertTrue(result.getSelf());
        assertTrue(CollUtil.isEmpty(result.getDeptIds()));
        assertSame(result, loginUser.getContext(PermissionServiceImpl.CONTEXT_KEY, DeptDataPermissionRespDTO.class));
    }

}
