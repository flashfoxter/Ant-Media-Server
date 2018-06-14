package io.antmedia.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;

public class HlsStatisticsFilter implements javax.servlet.Filter {

	protected static Logger logger = LoggerFactory.getLogger(HlsStatisticsFilter.class);
	private IStreamStats streamStats;
	private FilterConfig filterConfig;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {


		HttpServletRequest httpRequest =(HttpServletRequest)request;

		String method = httpRequest.getMethod();
		if (method.equals("GET")) {
			//only accept GET methods
			String sessionId = httpRequest.getSession().getId();

		
			chain.doFilter(request, response);

			int status = ((HttpServletResponse) response).getStatus();
			
			if (HttpServletResponse.SC_OK <= status && status <= HttpServletResponse.SC_BAD_REQUEST) 
			{
				String streamId = getStreamId(httpRequest.getRequestURI());
				
				if (streamId != null) {
					logger.info("session id {} stream id {} status {}", sessionId, streamId, status);
					getStreamStats().registerNewViewer(streamId, sessionId);
				}
			}
		}
		else {
			chain.doFilter(httpRequest, response);
		}

	}



	@Override
	public void destroy() {
		//There is no need to implement destroy right now
	}

	public static String getStreamId(String requestURI) {

		int startIndex = requestURI.lastIndexOf('/');

		//if request is adaptive file ( ending with _adaptive.m3u8)
		int endIndex = requestURI.lastIndexOf(MuxAdaptor.ADAPTIVE_SUFFIX + ".m3u8");
		if (endIndex != -1) {
			return requestURI.substring(startIndex+1, endIndex);
		}

		//if specific bitrate is requested
		endIndex = requestURI.lastIndexOf("p.m3u8");
		if (endIndex != -1) {
			endIndex = requestURI.lastIndexOf('_'); //because file format is [NAME]_[RESOLUTION]p.m3u8
			return requestURI.substring(startIndex+1, endIndex);
		}

		//if just the m3u8 file
		endIndex = requestURI.lastIndexOf(".m3u8");
		if (endIndex != -1) {
			return requestURI.substring(startIndex+1, endIndex);
		}
		return null;
	}

	public IStreamStats getStreamStats() {
		if (streamStats == null) {
			ApplicationContext context = (ApplicationContext) filterConfig.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			streamStats = (IStreamStats)context.getBean(HlsViewerStats.BEAN_NAME);

		}
		return streamStats;
	}

}
