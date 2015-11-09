package com.spinn3r.noxy.forward.init;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.spinn3r.artemis.init.BaseService;
import com.spinn3r.artemis.init.Config;
import com.spinn3r.artemis.init.advertisements.Hostname;
import com.spinn3r.artemis.util.net.HostPort;
import com.spinn3r.noxy.discovery.*;
import com.spinn3r.noxy.logging.Log5jLogListener;
import com.spinn3r.noxy.logging.LoggingHttpFiltersSourceAdapter;
import com.spinn3r.noxy.logging.LoggingHttpFiltersSourceAdapterFactory;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

/**
 *
 */
@Config( path = "forward-proxy.conf",
         required = true,
         implementation = ForwardProxyConfig.class )
public class ForwardProxyService extends BaseService {

    private final ForwardProxyConfig forwardProxyConfig;

    private final LoggingHttpFiltersSourceAdapterFactory loggingHttpFiltersSourceAdapterFactory;

    private final List<HttpProxyServer> httpProxyServers = Lists.newArrayList();

    private final MembershipFactory membershipFactory;

    private final Provider<Hostname> hostnameProvider;

    private Membership membership = null;

    @Inject
    ForwardProxyService(ForwardProxyConfig forwardProxyConfig, LoggingHttpFiltersSourceAdapterFactory loggingHttpFiltersSourceAdapterFactory, MembershipFactory membershipFactory, Provider<Hostname> hostnameProvider) {
        this.forwardProxyConfig = forwardProxyConfig;
        this.loggingHttpFiltersSourceAdapterFactory = loggingHttpFiltersSourceAdapterFactory;
        this.membershipFactory = membershipFactory;
        this.hostnameProvider = hostnameProvider;
    }

    @Override
    public void start() throws Exception {

        List<ProxyServerDescriptor> servers = forwardProxyConfig.getServers();

        if ( servers == null || servers.size() == 0 ) {
            warn( "No servers defined." );
            return;
        }

        if ( forwardProxyConfig.getCluster() != null ) {
            membership = membershipFactory.create( forwardProxyConfig.getCluster() );
        }

        HttpProxyServer proto = create( DefaultHttpProxyServer.bootstrap(), servers.get( 0 ) );
        httpProxyServers.add( proto );

        for (int i = 1; i < servers.size(); i++) {
            ProxyServerDescriptor proxyServerDescriptor = servers.get( i );
            httpProxyServers.add( create( proto.clone(), proxyServerDescriptor ) );
        }

    }

    private HttpProxyServer create( HttpProxyServerBootstrap httpProxyServerBootstrap, ProxyServerDescriptor proxyServerDescriptor ) throws MembershipException {

        info( "Creating proxy server: %s", proxyServerDescriptor );

        HostPort addressHostPort = new HostPort( proxyServerDescriptor.getInbound().getAddress(), proxyServerDescriptor.getInbound().getPort() );

        InetSocketAddress address = new InetSocketAddress( addressHostPort.getHostname(), addressHostPort.getPort() );
        InetSocketAddress networkInterface = new InetSocketAddress( proxyServerDescriptor.getOutbound().getAddress(), proxyServerDescriptor.getOutbound().getPort() );

        String name = proxyServerDescriptor.getName();

        if ( name == null ) {
            name = proxyServerDescriptor.getInbound().getAddress() + ":" + proxyServerDescriptor.getInbound().getPort();
        }

        // TODO: use a custom HostResolver for ipv6 and then one for ipv4 depending
        // on which mode a connection is taking.

        httpProxyServerBootstrap
          .withName( name )
          .withAddress( address )
          .withNetworkInterface( networkInterface );

        if ( forwardProxyConfig.getEnableRequestLogging() ) {
            Log5jLogListener log5jLogListener = new Log5jLogListener();
            LoggingHttpFiltersSourceAdapter loggingHttpFiltersSourceAdapter = loggingHttpFiltersSourceAdapterFactory.create( log5jLogListener );
            httpProxyServerBootstrap.withFiltersSource( loggingHttpFiltersSourceAdapter );
        }

        HttpProxyServer httpProxyServer = httpProxyServerBootstrap.start();

        if ( membership != null ) {

            Endpoint endpoint = new Endpoint( addressHostPort.format(),
                                              hostnameProvider.get().getValue(),
                                              EndpointType.FORWARD_PROXY,
                                              forwardProxyConfig.getDatacenter() );

            membership.join( endpoint );

        }

        return httpProxyServer;

    }

    @Override
    public void stop() throws Exception {

        List<HttpProxyServer> httpProxyServersReversed = Lists.newArrayList( httpProxyServers );
        Collections.reverse( httpProxyServersReversed );

        for (HttpProxyServer httpProxyServer : httpProxyServersReversed ) {
            httpProxyServer.stop();
        }

    }

}