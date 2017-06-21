package navriders.stuttgart.uni.com.example.mdand.navriders;

import java.util.List;

public interface RoutingListener {

    void onRoutingFailure(RouteException e);

    void onRoutingStart();

    void onRoutingSuccess(List<Route> route, int shortestRouteIndex);

    void onRoutingCancelled();
}
