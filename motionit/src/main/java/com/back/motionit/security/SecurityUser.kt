package com.back.motionit.security;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import lombok.Getter;

@Getter
public class SecurityUser extends User implements OAuth2User {

	private Long id;
	private String nickname;

	public SecurityUser(Long id, String password, String nickname, Collection<? extends GrantedAuthority> authorities) {
		super(nickname, password, authorities);
		this.id = id;
		this.nickname = nickname;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return Map.of();
	}

	@Override
	public String getName() {
		return nickname;
	}
}
