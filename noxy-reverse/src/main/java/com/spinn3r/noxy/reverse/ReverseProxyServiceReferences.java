package com.spinn3r.noxy.reverse;

import com.spinn3r.artemis.http.init.DefaultWebserverReferencesService;
import com.spinn3r.artemis.http.init.WebserverService;
import com.spinn3r.artemis.init.ServiceReferences;
import com.spinn3r.artemis.init.services.HostnameService;
import com.spinn3r.artemis.init.services.VersionService;
import com.spinn3r.artemis.logging.init.LoggingService;
import com.spinn3r.artemis.metrics.init.GlobalMetricsService;
import com.spinn3r.artemis.sequence.init.SequenceSupportService;
import com.spinn3r.noxy.discovery.support.init.DiscoveryListenerSupportService;
import com.spinn3r.noxy.reverse.admin.init.ReverseProxyAdminWebserverReferencesService;
import com.spinn3r.noxy.reverse.init.ReverseProxyService;
import com.spinn3r.artemis.sequence.none.init.NoGlobalMutexService;
import com.spinn3r.artemis.time.init.SystemClockService;
import com.spinn3r.artemis.time.init.UptimeService;

/**
 *
 */
public class ReverseProxyServiceReferences extends ServiceReferences {

    public ReverseProxyServiceReferences() {
        add( SystemClockService.class );
        add( HostnameService.class );
        add( VersionService.class );
        add( LoggingService.class );
        add( SequenceSupportService.class );
        add( GlobalMetricsService.class );
        add( UptimeService.class );
        add( DefaultWebserverReferencesService.class );
        add( DiscoveryListenerSupportService.class );
        add( ReverseProxyService.class );
        add( ReverseProxyAdminWebserverReferencesService.class );
        add( WebserverService.class );
    }

}
