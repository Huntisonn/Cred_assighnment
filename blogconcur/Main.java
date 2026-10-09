
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


class User {
    private final int userId;
    private final String name;
    private final String email;

    public User(int userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}



enum PostStatus {
    DRAFT,
    PUBLISHED,
    DELETED
}

class BlogPost {
    private final int postId;
    private final int authorId;

    private String title;
    private String content;
    private PostStatus status;

    private final Set<Integer> likedBy =
            ConcurrentHashMap.newKeySet();

    public BlogPost(int postId, int authorId,
                    String title, String content) {
        this.postId = postId;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.status = PostStatus.DRAFT;
    }

    public int getPostId() {
        return postId;
    }

    public int getAuthorId() {
        return authorId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public PostStatus getStatus() {
        return status;
    }

    public int getLikeCount() {
        return likedBy.size();
    }

    public boolean hasLiked(int userId) {
        return likedBy.contains(userId);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }

    public boolean addLike(int userId) {
        return likedBy.add(userId);
    }

    public boolean removeLike(int userId) {
        return likedBy.remove(userId);
    }
}


class BlogService {

    private final ConcurrentMap<Integer, User> users =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<Integer, BlogPost> posts =
            new ConcurrentHashMap<>();

    private final AtomicInteger nextPostId =
            new AtomicInteger(1);

  
    public boolean registerUser(int userId,
                                String name,
                                String email) {

        if (userId <= 0 ||
                name == null || name.trim().isEmpty() ||
                email == null || email.trim().isEmpty()) {
            return false;
        }

        User user = new User(userId, name, email);

        return users.putIfAbsent(userId, user) == null;
    }


    public int createPost(int authorId,
                          String title,
                          String content) {

        if (!users.containsKey(authorId) ||
                title == null || title.trim().isEmpty() ||
                content == null || content.trim().isEmpty()) {
            return -1;
        }

        int postId = nextPostId.getAndIncrement();

        BlogPost post = new BlogPost(
                postId, authorId, title, content);

        posts.put(postId, post);

        return postId;
    }

  
    public boolean publishPost(int userId, int postId) {

        BlogPost post = posts.get(postId);

        if (!users.containsKey(userId) || post == null) {
            return false;
        }

        synchronized (post) {
            if (post.getAuthorId() != userId ||
                    post.getStatus() != PostStatus.DRAFT) {
                return false;
            }

            post.setStatus(PostStatus.PUBLISHED);
            return true;
        }
    }


    public boolean editPost(int userId, int postId,
                            String newTitle,
                            String newContent) {

        BlogPost post = posts.get(postId);

        if (!users.containsKey(userId) || post == null ||
                newTitle == null || newTitle.trim().isEmpty() ||
                newContent == null || newContent.trim().isEmpty()) {
            return false;
        }

        synchronized (post) {
            if (post.getAuthorId() != userId ||
                    post.getStatus() == PostStatus.DELETED) {
                return false;
            }

            post.setTitle(newTitle);
            post.setContent(newContent);
            return true;
        }
    }


    public boolean deletePost(int userId, int postId) {

        BlogPost post = posts.get(postId);

        if (!users.containsKey(userId) || post == null) {
            return false;
        }

        synchronized (post) {
            if (post.getAuthorId() != userId ||
                    post.getStatus() == PostStatus.DELETED) {
                return false;
            }

            post.setStatus(PostStatus.DELETED);
            return true;
        }
    }

    public boolean likePost(int userId, int postId) {

        if (!users.containsKey(userId)) {
            return false;
        }

        BlogPost post = posts.get(postId);

        if (post == null) {
            return false;
        }

        synchronized (post) {
            if (post.getStatus() != PostStatus.PUBLISHED) {
                return false;
            }

            return post.addLike(userId);
        }
    }

    public boolean unlikePost(int userId, int postId) {

        if (!users.containsKey(userId)) {
            return false;
        }

        BlogPost post = posts.get(postId);

        if (post == null) {
            return false;
        }

        synchronized (post) {
            if (post.getStatus() != PostStatus.PUBLISHED) {
                return false;
            }

            return post.removeLike(userId);
        }
    }


    public List<BlogPost> getPublishedPosts() {

        List<BlogPost> result = new ArrayList<>();

        for (BlogPost post : posts.values()) {
            synchronized (post) {
                if (post.getStatus() == PostStatus.PUBLISHED) {
                    result.add(post);
                }
            }
        }

        return result;
    }

    // Get posts by author
    public List<BlogPost> getPostsByAuthor(int authorId) {

        List<BlogPost> result = new ArrayList<>();

        for (BlogPost post : posts.values()) {
            synchronized (post) {
                if (post.getAuthorId() == authorId &&
                        post.getStatus() != PostStatus.DELETED) {
                    result.add(post);
                }
            }
        }

        return result;
    }

    // Get a single post
    public Optional<BlogPost> getPost(int postId) {

        BlogPost post = posts.get(postId);

        if (post == null) {
            return Optional.empty();
        }

        synchronized (post) {
            if (post.getStatus() == PostStatus.DELETED) {
                return Optional.empty();
            }

            return Optional.of(post);
        }
    }
}



public class Main {

    public static void main(String[] args)
            throws InterruptedException {

        BlogService service = new BlogService();

        service.registerUser(1, "Rahul", "rahul@example.com");
        service.registerUser(2, "Priya", "priya@example.com");
        service.registerUser(3, "Amit", "amit@example.com");

        int postId = service.createPost(
                1,
                "Getting Started with Java",
                "Java is an object-oriented language."
        );

        System.out.println("Post ID: " + postId);

        System.out.println(
                "Published: " + service.publishPost(1, postId));

        
        ExecutorService executor =
                Executors.newFixedThreadPool(3);

        executor.submit(() ->
                System.out.println(
                        "Rahul liked: " +
                        service.likePost(1, postId)));

        executor.submit(() ->
                System.out.println(
                        "Priya liked: " +
                        service.likePost(2, postId)));

        executor.submit(() ->
                System.out.println(
                        "Amit liked: " +
                        service.likePost(3, postId)));

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        Optional<BlogPost> post = service.getPost(postId);

        post.ifPresent(p -> {
            System.out.println("Title: " + p.getTitle());
            System.out.println("Status: " + p.getStatus());
            System.out.println("Likes: " + p.getLikeCount());
        });
    }
}
