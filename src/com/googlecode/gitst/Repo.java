package com.googlecode.gitst;

import static com.googlecode.gitst.RepoProperties.PROP_BRANCH;
import static com.googlecode.gitst.RepoProperties.PROP_DEFAULT_BRANCH;
import static com.googlecode.gitst.RepoProperties.PROP_DEFAULT_USER_NAME_PATTERN;
import static com.googlecode.gitst.RepoProperties.PROP_HOST;
import static com.googlecode.gitst.RepoProperties.PROP_PASSWORD;
import static com.googlecode.gitst.RepoProperties.PROP_PORT;
import static com.googlecode.gitst.RepoProperties.PROP_PROJECT;
import static com.googlecode.gitst.RepoProperties.PROP_USER;
import static com.googlecode.gitst.RepoProperties.PROP_USER_NAME_PATTERN;
import static com.googlecode.gitst.RepoProperties.PROP_VIEW;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.starbase.starteam.Project;
import com.starbase.starteam.Server;
import com.starbase.starteam.User;
import com.starbase.starteam.View;

/**
 * @author Andrey Pavlenko
 */
public class Repo implements AutoCloseable {
    private final RepoProperties _repoProperties;
    private final String _host;
    private final int _port;
    private final String _projectName;
    private final String _viewName;
    private final String _userName;
    private final String _password;
    private final String _branchName;
    private final String _userNamePattern;
    private final Git _git;
    private Server _server;
    private Project _project;
    private View _view;
    private int _currentUserId;

    public Repo(final RepoProperties repoProperties) {
        String s = repoProperties.getProperty(PROP_USER, null);

        if (s == null) {
            _userName = repoProperties.requestProperty(PROP_USER, "Username: ",
                    false);
        } else {
            _userName = s;
        }

        s = repoProperties.getProperty(PROP_PASSWORD, null);

        if (s == null) {
            _password = repoProperties.requestProperty(PROP_PASSWORD,
                    "Password: ", true);
        } else {
            _password = s;
        }

        _host = repoProperties.getProperty(PROP_HOST);
        _port = Integer.parseInt(repoProperties.getProperty(PROP_PORT));
        _projectName = repoProperties.getProperty(PROP_PROJECT);
        _viewName = repoProperties.getProperty(PROP_VIEW);
        _branchName = repoProperties.getProperty(PROP_BRANCH,
                PROP_DEFAULT_BRANCH);
        _userNamePattern = repoProperties.getProperty(PROP_USER_NAME_PATTERN,
                PROP_DEFAULT_USER_NAME_PATTERN);
        _repoProperties = repoProperties;
        _git = new Git(repoProperties.getRepoDir());
    }

    public synchronized View connect() {
        if (_server == null) {
            _server = new Server(getHost(), getPort());
            _server.connect();
            _currentUserId = _server.logOn(getUserName(), getPassword());

            if (_currentUserId == 0) {
                throw new ConfigurationException("Failed to login to "
                        + getHost() + ':' + getPort() + " as user "
                        + getUserName());
            }

            _project = findProject(_server, getProjectName());
            _view = findView(_project, getViewName());
        }

        return _view;
    }

    @Override
    public synchronized void close() {
        if (_server != null) {
            final Server s = _server;
            _server = null;
            s.disconnect();
        }
    }

    public RepoProperties getRepoProperties() {
        return _repoProperties;
    }

    public String getHost() {
        return _host;
    }

    public int getPort() {
        return _port;
    }

    public String getProjectName() {
        return _projectName;
    }

    public String getViewName() {
        return _viewName;
    }

    public String getBranchName() {
        return _branchName;
    }

    public String getUserName() {
        return _userName;
    }

    public String getPassword() {
        return _password;
    }

    public String getUserNamePattern() {
        return _userNamePattern;
    }

    public Server getServer() {
        return _server;
    }

    public synchronized Project getProject() {
        return _project;
    }

    public synchronized View getView() {
        return _view;
    }

    public synchronized int getCurrentUserId() {
        return _currentUserId;
    }

    public Git getGit() {
        return _git;
    }

    public boolean isBare() {
        RepoProperties props = getRepoProperties();
        return props.getGitstDir().getParent().equals(props.getRepoDir());
    }

    public String toCommitter(final int userId) {
        String name = getRepoProperties().getUserMapping(userId);

        if (name != null) {
            return name;
        }

        final View v = getView();

        if (v != null) {
            final User user = getServer().getUser(userId);

            if (user != null) {
                name = user.getName();
            }
        }
        if (name == null) {
            name = "Unknown User";
        }

        final List<String> l = new ArrayList<>();
        l.add(name);

        for (final StringTokenizer st = new StringTokenizer(name, " ,"); st
                .hasMoreTokens();) {
            l.add(st.nextToken());
        }

        name = MessageFormat.format(getUserNamePattern(), l.toArray());
        getRepoProperties().setSessionUserMapping(userId, name);
        return name;
    }

    private static Project findProject(final Server s, final String name) {
        for (final Project p : s.getProjects()) {
            if (name.equals(p.getName())) {
                return p;
            }
        }

        throw new ConfigurationException("No such project: " + name);
    }

    private static View findView(final Project p, final String name) {
        for (final View v : p.getViews()) {
            if (name.equals(v.getName())) {
                return v;
            }
        }

        throw new ConfigurationException("No such view: " + name);
    }
}
