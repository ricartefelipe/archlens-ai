package dev.archlens.infrastructure.messaging;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AnalysisProducer {

    private static final Logger LOG = Logger.getLogger(AnalysisProducer.class);

    @Inject
    @Channel("analysis-requests-out")
    Emitter<AnalysisEvent> emitter;

    public void sendAnalysisRequest(AnalysisEvent event) {
        LOG.infof("Publishing analysis request: analysisId=%s, projectId=%s", event.analysisId(), event.projectId());
        emitter.send(event);
    }
}
