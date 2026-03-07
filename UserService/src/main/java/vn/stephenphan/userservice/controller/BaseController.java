package vn.stephenphan.userservice.controller;

import vn.stephenphan.userservice.dto.UserPrincipal;
import vn.stephenphan.userservice.utils.SecurityUtils;

public abstract class BaseController {

    protected UserPrincipal currentUser() {
        return SecurityUtils.getCurrentUser();
    }

    protected String getUserId() {
        return currentUser().userId();
    }
}