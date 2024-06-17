package allen.town.focus.twitter.model;

public class CacheablePaginatedResponse<T> extends PaginatedResponse<T>{
	private final boolean fromCache;

	public CacheablePaginatedResponse(T items, String maxID, boolean fromCache){
		super(items, maxID);
		this.fromCache=fromCache;
	}

	public boolean isFromCache(){
		return fromCache;
	}
}
