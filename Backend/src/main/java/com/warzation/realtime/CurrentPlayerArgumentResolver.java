package com.warzation.realtime;

import com.warzation.application.player.Player;
import com.warzation.application.player.PlayerDirectory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentPlayerArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String BEARER = "Bearer ";

    private final PlayerDirectory players;

    public CurrentPlayerArgumentResolver(PlayerDirectory players) {
        this.players = players;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentPlayer.class) && Player.class.equals(parameter.getParameterType());
    }

    @Override
    public Player resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String token = header != null && header.regionMatches(true, 0, BEARER, 0, BEARER.length())
                ? header.substring(BEARER.length())
                : null;
        return players.require(token);
    }
}
