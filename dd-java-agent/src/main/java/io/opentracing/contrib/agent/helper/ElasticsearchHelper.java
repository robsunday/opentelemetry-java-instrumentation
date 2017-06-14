package io.opentracing.contrib.agent.helper;

import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.elasticsearch.TracingPreBuiltTransportClient;
import io.opentracing.contrib.elasticsearch.TracingResponseListener;
import io.opentracing.tag.Tags;
import org.elasticsearch.action.ActionListener;
import org.jboss.byteman.rule.Rule;

import java.lang.reflect.Method;


public class ElasticsearchHelper extends DDTracingHelper<ActionListener> {

	public ElasticsearchHelper(Rule rule) {
		super(rule);
	}

	private Object request;

	public void registerArgs(Object request) {
		this.request = request;
	}


	public ActionListener patch(ActionListener listener) {
		return super.patch(listener);
	}


	@Override
	protected  ActionListener doPatch(ActionListener listener) throws Exception {


		Tracer.SpanBuilder spanBuilder = this.tracer.buildSpan(request.getClass().getSimpleName()).ignoreActiveSpan().withTag(Tags.SPAN_KIND.getKey(), "client");
		ActiveSpan parentSpan = this.tracer.activeSpan();
		if(parentSpan != null) {
			spanBuilder.asChildOf(parentSpan.context());
		}

		Span span = spanBuilder.startManual();

		Class<?> clazz = Class.forName("io.opentracing.contrib.elasticsearch.SpanDecorator");
		Method method = clazz.getMethod("onRequest", Span.class);
		method.invoke(null, span);

		ActionListener newListener = new TracingResponseListener(listener, span);
		return newListener;

	}
}